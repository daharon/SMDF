/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.cli

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.*
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import freemarker.template.Configuration as TemplateConfiguration
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import us.aharon.smdf.core.checks.notificationHandlerParameterPath
import us.aharon.smdf.core.checks.notificationHandlerPermissions
import us.aharon.smdf.core.checks.serverlessExecutorParameterPath
import us.aharon.smdf.core.checks.serverlessExecutorPermissions
import us.aharon.smdf.core.util.getJarAbsolutePath
import us.aharon.smdf.core.util.getJarFile
import us.aharon.smdf.core.util.toMd5HexString

import java.io.StringWriter
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


/**
 * CLI sub-command to deploy the application to the cloud.
 */
@Command(name = "deploy",
        description = ["Deploy application to the cloud"],
        mixinStandardHelpOptions = true)
internal class Deploy : Runnable {

    @ParentCommand
    private lateinit var parent: Base

    /**
     * Command-line parameters.
     */
    @Option(names = ["--dry-run"],
            description = ["Generate and validate the CloudFormation template without installing the application"],
            required = false)
    private var dryRun: Boolean = false

    @Option(names = ["-r", "--region"],
            paramLabel = "REGION",
            description = ["AWS region"],
            required = true,
            converter = [AWSRegionConverter::class])
    private lateinit var region: Regions

    @Option(names = ["-d", "--s3-dest"],
            paramLabel = "DEST",
            description = ["S3 Bucket and path as upload destination. eg. <bucket>/path/"],
            required = true,
            converter = [AWSS3PathConverter::class])
    private lateinit var s3Dest: S3BucketAndPath

    @Option(names = ["-n", "--stack-name"],
            paramLabel = "NAME",
            description = ["CloudFormation Stack name"],
            required = true)
    private lateinit var stackName: String

    @Option(names = ["-e", "--environment"],
            paramLabel = "ENV",
            description = ["A name given to the environment for this application (prd, dev, ...)"],
            required = true)
    private lateinit var environment: String

    @Option(names = ["-l", "--log-level"],
            paramLabel = "LEVEL",
            description = ["Log level (TRACE, DEBUG, ERROR, WARN, INFO)"],
            required = false)
    private var logLevel: String = "INFO"

    @ArgGroup(heading = "VPC%n", exclusive = false)
    private var vpc: Vpc = Vpc()

    companion object {
        /**
         * Co-dependent VPC options.
         */
        private class Vpc {
            @Option(names = ["--security-group-ids"],
                    paramLabel = "SECURITY_GROUP",
                    description = ["List of security group IDs for notification and serverless check functions."],
                    split = ",",
                    required = true)
            var securityGroups: MutableList<String> = mutableListOf()

            @Option(names = ["--subnet-ids"],
                    paramLabel = "SUBNET",
                    description = ["List of subnet IDs for notification and serverless check functions."],
                    split = ",",
                    required = true)
            var subnets: MutableList<String> = mutableListOf()
        }

        /**
         * CloudFormation template filename from the package's resources directory.
         */
        private const val CLOUDFORMATION_TEMPLATE = "cloudformation.yaml"
    }

    /**
     * Deploy the application to the cloud.
     */
    override fun run() {
        // Get MD5 hash of the JAR file and use it as the filename on S3.
        val jarS3Filename = "${getJarFile(parent.app::class).toMd5HexString()}.jar"
        // Generate/render CloudFormation template.
        val cfnTemplate = renderCloudFormationTemplate(jarS3Filename)
        println("Rendered CloudFormation Template:")
        println(cfnTemplate)

        // Validate CloudFormation template.
        validateCloudFormationTemplate(cfnTemplate)

        if (!dryRun) {
            // Upload JAR to S3 bucket specified in command-line parameter.
            uploadJarFile(jarS3Filename)

            // Upload CloudFormation template to S3 bucket.
            val cfnTemplateS3Key = uploadCloudFormationTemplate(cfnTemplate)
            TimeUnit.SECONDS.sleep(1)  // Wait for S3, just in case.

            // Create or update CloudFormation stack.  Stack name provided by command-line parameter.
            createCloudFormationStack(cfnTemplateS3Key)

            // Poll for stack creation/update status.
            pollCloudFormationStackStatus()
        }
    }

    /**
     * Print the stack creation/update status until an end state is reached.
     */
    private fun pollCloudFormationStackStatus() {
        val cfnClient = AmazonCloudFormationClientBuilder.standard()
                .withRegion(region)
                .build()
        val stackReq = DescribeStacksRequest().withStackName(stackName)
        while (true) {
            val stacksResult = cfnClient.describeStacks(stackReq)
            val stack = stacksResult.stacks.firstOrNull()
            if (stack == null) {
                println("Stack `$stackName` not found.")
            } else {
                when (StackStatus.fromValue(stack.stackStatus)) {
                    StackStatus.CREATE_COMPLETE,
                    StackStatus.DELETE_COMPLETE,
                    StackStatus.ROLLBACK_COMPLETE,
                    StackStatus.UPDATE_COMPLETE,
                    StackStatus.UPDATE_ROLLBACK_COMPLETE -> {
                        println("Stack `$stackName` status:  ${stack.stackStatus}")
                        return
                    }
                    StackStatus.CREATE_IN_PROGRESS,
                    StackStatus.DELETE_IN_PROGRESS,
                    StackStatus.REVIEW_IN_PROGRESS,
                    StackStatus.ROLLBACK_IN_PROGRESS,
                    StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
                    StackStatus.UPDATE_IN_PROGRESS,
                    StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
                    StackStatus.UPDATE_ROLLBACK_IN_PROGRESS -> {
                        println("Stack `$stackName` status:  ${stack.stackStatus}")
                        TimeUnit.SECONDS.sleep(5)
                    }
                    StackStatus.CREATE_FAILED,
                    StackStatus.DELETE_FAILED,
                    StackStatus.ROLLBACK_FAILED,
                    StackStatus.UPDATE_ROLLBACK_FAILED -> {
                        println("Stack `$stackName` status:  ${stack.stackStatus} - ${stack.stackStatusReason}")
                        return
                    }
                    null -> {
                        println("Stack `$stackName` status is NULL.")
                    }
                }
            }
        }
    }

    /**
     * Create or update the CloudFormation stack.
     *
     * @param template The S3 file/key that contains the CloudFormation template.
     */
    private fun createCloudFormationStack(template: String) {
        val cfnClient = AmazonCloudFormationClientBuilder.standard()
                .withRegion(region)
                .build()
        val stackExists: Boolean = try {
            val describeStackRequest = DescribeStacksRequest().withStackName(stackName)
            cfnClient.describeStacks(describeStackRequest)
            println("The `$stackName` stack already exists.")
            true
        } catch (e: AmazonCloudFormationException) {
            println("The `$stackName` stack does not exist.")
            false
        }
        val templateUrl = "https://${s3Dest.bucket}.s3.amazonaws.com/$template"
        println("Template URL:  $templateUrl")
        if (stackExists) {
            val updateRequest = UpdateStackRequest()
                    .withStackName(stackName)
                    .withTemplateURL(templateUrl)
                    .withCapabilities(Capability.CAPABILITY_IAM, Capability.CAPABILITY_NAMED_IAM)
            println("Updating the stack named `$stackName`...")
            cfnClient.updateStack(updateRequest)
        } else {
            val createRequest = CreateStackRequest()
                    .withStackName(stackName)
                    .withTemplateURL(templateUrl)
                    .withCapabilities(Capability.CAPABILITY_IAM, Capability.CAPABILITY_NAMED_IAM)
            println("Creating the stack named `$stackName`...")
            cfnClient.createStack(createRequest)
        }
    }

    /**
     * Upload CloudFormation template.
     *
     * @param template CloudFormation template.
     * @return Template S3 path/key.
     */
    private fun uploadCloudFormationTemplate(template: String): String {
        val templateKey = "${s3Dest.path}/$environment-${template.toMd5HexString()}.yaml"
        println("Uploading CloudFormation template to s3://${s3Dest.bucket}/$templateKey ...")
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build()
        s3Client.putObject(s3Dest.bucket, templateKey, template)
        return templateKey
    }

    /**
     * Upload the JAR file.
     */
    private fun uploadJarFile(jarFilename: String) {
        val jarPath = getJarAbsolutePath(parent.app::class)
        val jarKey = "${s3Dest.path}/$jarFilename"
        println("Uploading $jarPath to s3://${s3Dest.bucket}/$jarKey ...")
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build()
        s3Client.putObject(s3Dest.bucket, jarKey, File(jarPath))
    }

    /**
     * Render the CloudFormation template.
     *
     * @return CloudFormation YAML template.
     */
    private fun renderCloudFormationTemplate(jarFilename: String): String {
        val templateConfig = TemplateConfiguration(TemplateConfiguration.VERSION_2_3_28).apply {
            setClassForTemplateLoading(this@Deploy.parent.app::class.java, "/")
            defaultEncoding = "UTF-8"
        }
        val templateCfn = templateConfig.getTemplate(CLOUDFORMATION_TEMPLATE)
        val templateData = mapOf<String, Any>(
                "environment" to environment,
                "logLevel" to logLevel.toUpperCase(),
                // Code
                "codeS3Bucket" to s3Dest.bucket,
                "codeS3Key" to "${s3Dest.path}/$jarFilename",
                // Serverless check executor permissions
                "serverlessExecutorPermissions" to parent.app.checks.serverlessExecutorPermissions(),
                "serverlessExecutorParameterPath" to serverlessExecutorParameterPath(environment),
                // Notification handler permissions
                "notificationHandlerPermissions" to parent.app.checks.notificationHandlerPermissions(),
                "notificationHandlerParameterPath" to notificationHandlerParameterPath(environment),
                // Functions
                "clientRegistrationHandler" to "${parent.app::class.java.canonicalName}::${parent.app::clientRegistration.name}",
                "clientDeregistrationHandler" to "${parent.app::class.java.canonicalName}::${parent.app::clientDeregistration.name}",
                "checkSchedulerHandler" to "${parent.app::class.java.canonicalName}::${parent.app::checkScheduler.name}",
                "checkResultReceiver" to "${parent.app::class.java.canonicalName}::${parent.app::checkResultReceiver.name}",
                "notificationProcessor" to "${parent.app::class.java.canonicalName}::${parent.app::notificationProcessor.name}",
                "serverlessCheckProcessor" to "${parent.app::class.java.canonicalName}::${parent.app::serverlessCheckProcessor.name}",
                "databaseStreamProcessor" to "${parent.app::class.java.canonicalName}::${parent.app::databaseStreamProcessor.name}",
                // VPC
                "securityGroups" to vpc.securityGroups,
                "subnets" to vpc.subnets
        )
        val renderedTemplate = StringWriter()
        templateCfn.process(templateData, renderedTemplate)
        return renderedTemplate.toString()
    }

    /**
     * Validate the CloudFormation template.
     *
     * @param template CloudFormation template to validate.
     */
    private fun validateCloudFormationTemplate(template: String) {
        val request = ValidateTemplateRequest().withTemplateBody(template)
        val client = AmazonCloudFormationClientBuilder.defaultClient()
        try {
            client.validateTemplate(request)
            println("CloudFormation template is valid.")
        } catch (e: AmazonCloudFormationException) {
            println(e.message)
            exitProcess(1)
        }
    }
}
