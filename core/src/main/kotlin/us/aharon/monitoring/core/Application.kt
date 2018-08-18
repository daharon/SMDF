/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core

import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import freemarker.template.Configuration as TemplateConfiguration

import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.http.RegistrationRequest
import us.aharon.monitoring.core.http.RegistrationResponse
import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.mutators.Mutator
import us.aharon.monitoring.core.util.CLIArgs

import java.io.StringWriter
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


    companion object {
        /**
         * CloudFormation template filename from the package's resources directory.
         */
        private const val CLOUDFORMATION_TEMPLATE = "cloudformation.yaml"
    }

    /**
     * API Gateway handler that registers a client for monitoring.
     *
     * - Verify that the client name is unique.
     * - Create SQS queue for the client subscriber with a filter based on the client's provided tags.
     *
     * @return Client SQS queue for receiving scheduled checks.  Success or failure is returned based on response code.
     */
    fun clientRegistrationHandler(request: RegistrationRequest, context: Context): RegistrationResponse {
        TODO("Implement client registration")
        return RegistrationResponse(
                "arn:aws:sqs:region:account-id:queuename")
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
        TODO("Implement check scheduler to fire events.")
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

        // Upload CloudFormation template to S3 bucket.

        // Create or update CloudFormation stack.  Stack name provided by command-line parameter.

        // Poll for stack creation/update status.
    }

    /**
     * Render the CloudFormation template.
     *
     * @param config Configuration variables.
     */
    private fun renderCloudFormationTemplate(options: CLIArgs): String {
        val templateConfig = TemplateConfiguration(TemplateConfiguration.VERSION_2_3_28).apply {
            setClassForTemplateLoading(this::class.java, "/")
            defaultEncoding = "UTF-8"
        }
        val templateCfn = templateConfig.getTemplate(CLOUDFORMATION_TEMPLATE)
        val templateData = mapOf<String, Any>(
                // TODO:  Figure out how to get the canonical name of the class that extends [Application].
                "clientRegistrationHandler" to "${this::class.java.canonicalName}::${this::clientRegistrationHandler.name}"
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
