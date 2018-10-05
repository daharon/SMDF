/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.cli

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.*
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import freemarker.template.Configuration as TemplateConfiguration
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import us.aharon.monitoring.core.http.ClientRegistrationHandler
import us.aharon.monitoring.core.util.getJarAbsolutePath
import us.aharon.monitoring.core.util.getJarFilename

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
    @Option(names = ["-r", "--region"],
            description = ["AWS region"],
            required = true,
            converter = [AWSRegionConverter::class])
    private lateinit var region: Regions

    @Option(names = ["-d", "--s3-dest"],
            description = ["S3 Bucket and path as upload destination. eg. <bucket>/path/"],
            required = true)
    private lateinit var s3Dest: String

    @Option(names = ["-n", "--stack-name"],
            description = ["CloudFormation Stack name"],
            required = true)
    private lateinit var stackName: String


    companion object {
        /**
         * CloudFormation template filename from the package's resources directory.
         */
        private const val CLOUDFORMATION_TEMPLATE = "cloudformation.yaml"
    }

    /**
     * Deploy the application to the cloud.
     */
    override fun run() {
        // Generate/render CloudFormation template.
        val cfnTemplate = renderCloudFormationTemplate()
        println("Rendered CloudFormation Template:")
        println(cfnTemplate)

        // Validate CloudFormation template.
        validateCloudFormationTemplate(cfnTemplate)

        // Upload JAR to S3 bucket specified in command-line parameter.
        uploadJarFile()

        // Upload CloudFormation template to S3 bucket.
        val cfnTemplateS3Key = uploadCloudFormationTemplate(cfnTemplate)
        TimeUnit.SECONDS.sleep(5)  // Wait for S3, just in case.

        // Create or update CloudFormation stack.  Stack name provided by command-line parameter.
        createCloudFormationStack(cfnTemplateS3Key)

        // Poll for stack creation/update status.
        pollCloudFormationStackStatus()
    }

    /**
     * Print the stack creation/update status until an end state is reached.
     */
    private fun pollCloudFormationStackStatus() {
        val cfnClient = AmazonCloudFormationClientBuilder.standard()
                .withRegion(region)
                .build()
        while (true) {
            val stacksResult = cfnClient.describeStacks()
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
     * @param template The S3 file that contains the CloudFormation template.
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
        val templateUrl = "https://s3.amazonaws.com/$s3Dest/$template"
        println("Template URL:  $templateUrl")
        if (stackExists) {
            val updateRequest = UpdateStackRequest()
                    .withStackName(stackName)
                    .withTemplateURL(templateUrl)
                    .withCapabilities(Capability.CAPABILITY_IAM)
            println("Updating the stack named `$stackName`...")
            cfnClient.updateStack(updateRequest)
        } else {
            val createRequest = CreateStackRequest()
                    .withStackName(stackName)
                    .withTemplateURL(templateUrl)
                    .withCapabilities(Capability.CAPABILITY_IAM)
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
        val templateFilename = "cfn-template-${System.currentTimeMillis()}.yaml"
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build()
        s3Client.putObject(s3Dest, templateFilename, template)
        return templateFilename
    }

    /**
     * Upload the JAR file.
     */
    private fun uploadJarFile() {
        val jarPath = getJarAbsolutePath(parent.app::class)
        val jarName = getJarFilename(parent.app::class)
        println("Uploading $jarPath")
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build()
        s3Client.putObject(s3Dest, jarName, File(jarPath))
    }

    /**
     * Render the CloudFormation template.
     *
     * @return CloudFormation YAML template.
     */
    private fun renderCloudFormationTemplate(): String {
        val templateConfig = TemplateConfiguration(TemplateConfiguration.VERSION_2_3_28).apply {
            setClassForTemplateLoading(this@Deploy.parent.app::class.java, "/")
            defaultEncoding = "UTF-8"
        }
        val templateCfn = templateConfig.getTemplate(CLOUDFORMATION_TEMPLATE)
        val templateData = mapOf<String, Any>(
                "clientRegistrationHandler" to
                        "${ClientRegistrationHandler::class.java.canonicalName}::${ClientRegistrationHandler::handleRequest.name}",
                "checkSchedulerHandler" to "${parent.app::class.java.canonicalName}::${parent.app::checkScheduler.name}",
                "clientCleanupHandler" to "${parent.app::class.java.canonicalName}::${parent.app::clientCleanup.name}",
                "checkResultReceiver" to "${parent.app::class.java.canonicalName}::${parent.app::checkResultReceiver.name}",
                "codeS3Bucket" to s3Dest,
                "codeS3Key" to getJarFilename(parent.app::class)
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
