/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core

import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.*
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import freemarker.template.Configuration as TemplateConfiguration
import mu.KLogger
import mu.KotlinLogging

import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.http.ClientRegistrationHandler
import us.aharon.monitoring.core.mutators.Mutator
import us.aharon.monitoring.core.util.CLIArgs
import us.aharon.monitoring.core.util.getJarAbsolutePath
import us.aharon.monitoring.core.util.getJarFilename

import java.io.StringWriter
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


/**
 * Base class for the monitoring application.
 * Extend this class and override its members in the project implementation.
 * ```
 * class MyMonitoringApp : Application() { ... }
 * ```
 * Provides the following:
 * - Command-line executable that performs installation of the monitoring application.
 * - Serverless function handlers.
 */
abstract class Application {

    abstract val checks: List<CheckGroup>
    abstract val filters: List<Filter>
    abstract val mutators: List<Mutator>

    protected val log: KLogger by lazy { KotlinLogging.logger(this::class.java.simpleName) }


    companion object {
        /**
         * CloudFormation template filename from the package's resources directory.
         */
        private const val CLOUDFORMATION_TEMPLATE = "cloudformation.yaml"
    }

    /**
     * Fire events to the SNS Fanout Topic.
     *
     * This handler is triggered by the CloudWatch scheduled event resource.
     * Examine all defined checks and push those whose [interval] % [current_minute] equals zero.
     *
     * Requires the ARN of the SNS Check Fanout topic.
     */
    fun checkScheduler(event: ScheduledEvent, context: Context) {
        log.info { "Event Time:  ${event.time}" }
        log.info { "Detail Type:  ${event.detailType}" }
        log.info { "Detail:  ${event.detail}" }
        log.info { "Resources:  ${event.resources}" }
        us.aharon.monitoring.core.backend.checkScheduler(event.time, this.checks)
    }

    /**
     * Receive scheduled check results and save to database.
     */
    fun checkResultReceiver(event: SQSEvent, context: Context) {
        TODO("Implement check result processor.")
    }

    /**
     * Process the check results as provided by the DynamoDB stream.
     *
     * - Determine if forwarding to the notification handler is necessary.
     * - Was there a state change?
     * - Flapping detection?
     */
    fun checkResultProcessor(event: DynamodbEvent, context: Context) {
        TODO("Implement check result processor.")
    }

    /**
     * Run the specified notification handler given the metadata passed along with the event.
     *
     * - Apply the application defined Filters and Mutators before forwarding to the notification handler.
     */
    fun notificationHandler(event: NotificationEvent, context: Context) {
        TODO("Run the notification handler class.")
    }

    /**
     * Entry point for the CLI.
     * In your application use like so:
     * ```
     * class MyMonitoringApp : Application() { ... }
     *
     * fun main(vararg args: String) {
     *     MyMonitoringApp().run(args)
     * }
     * ```
     */
    fun run(args: Array<out String>) {
        // Parse command line parameters.
        val options = CLIArgs(args)

        // Generate/render CloudFormation template.
        val cfnTemplate = renderCloudFormationTemplate(options)
        println("Rendered CloudFormation Template:")
        println(cfnTemplate)

        // Validate CloudFormation template.
        validateCloudFormationTemplate(cfnTemplate)

        // Upload JAR to S3 bucket specified in command-line parameter.
        uploadJarFile(options)

        // Upload CloudFormation template to S3 bucket.
        val cfnTemplateS3Key = uploadCloudFormationTemplate(cfnTemplate, options)
        TimeUnit.SECONDS.sleep(5)  // Wait for S3, just in case.

        // Create or update CloudFormation stack.  Stack name provided by command-line parameter.
        createCloudFormationStack(options.get("stack-name"), options.get("s3-dest"), cfnTemplateS3Key, options.get("region"))

        // Poll for stack creation/update status.
        pollCloudFormationStackStatus(options.get("stack-name"), options.get("region"))
    }

    /**
     * Print the stack creation/update status until an end state is reached.
     *
     * @param stackName The name of the CloudFormation stack.
     * @param region The AWS region.
     */
    private fun pollCloudFormationStackStatus(stackName: String, region: String) {
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
     * @param stackName The name of the CloudFormation stack.
     * @param template The S3 file that contains the CloudFormation template.
     * @param region The AWS region.
     */
    private fun createCloudFormationStack(stackName: String, s3Bucket: String, template: String, region: String) {
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
        val templateUrl = "https://s3.amazonaws.com/$s3Bucket/$template"
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
     * @param options Configuration variables.
     * @return Template S3 path/key.
     */
    private fun uploadCloudFormationTemplate(template: String, options: CLIArgs): String {
        val templateFilename = "cfn-template-${System.currentTimeMillis()}.yaml"
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(options.get("region"))
                .build()
        s3Client.putObject(options.get("s3-dest"), templateFilename, template)
        return templateFilename
    }

    /**
     * Upload the JAR file.
     *
     * @param options Configuration variables.
     */
    private fun uploadJarFile(options: CLIArgs) {
        val jarPath = getJarAbsolutePath(this::class)
        val jarName = getJarFilename(this::class)
        println("Uploading $jarPath")
        val s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(options.get("region"))
                .build()
        s3Client.putObject(options.get("s3-dest"), jarName, File(jarPath))
    }

    /**
     * Render the CloudFormation template.
     *
     * @param options Configuration variables.
     */
    private fun renderCloudFormationTemplate(options: CLIArgs): String {
        val templateConfig = TemplateConfiguration(TemplateConfiguration.VERSION_2_3_28).apply {
            setClassForTemplateLoading(this::class.java, "/")
            defaultEncoding = "UTF-8"
        }
        val templateCfn = templateConfig.getTemplate(CLOUDFORMATION_TEMPLATE)
        val templateData = mapOf<String, Any>(
                // TODO:  Figure out how to get the canonical name of the class that extends [Application].
                "clientRegistrationHandler" to
                        "${ClientRegistrationHandler::class.java.canonicalName}::${ClientRegistrationHandler::handleRequest.name}",
                "checkSchedulerHandler" to "${this::class.java.canonicalName}::${this::checkScheduler.name}",
                "codeS3Bucket" to options.get("s3-dest"),
                "codeS3Key" to getJarFilename(this::class)
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
