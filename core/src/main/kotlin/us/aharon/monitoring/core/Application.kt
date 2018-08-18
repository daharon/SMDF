package us.aharon.monitoring.core

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent

import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.http.RegistrationRequest
import us.aharon.monitoring.core.http.RegistrationResponse
import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.mutators.Mutator
import us.aharon.monitoring.core.util.CLIArgs
import us.aharon.monitoring.core.util.renderCloudFormationTemplate


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
        @JvmStatic
        fun main(vararg args: String) {
            // Parse command line parameters.
            val options = CLIArgs(args)

            // Generate/render CloudFormation template.
            val cfnTemplate = renderCloudFormationTemplate(this::class, options)
            println("Rendered CloudFormation Template:")
            println(cfnTemplate)

            // Validate CloudFormation template.

            // Upload JAR to S3 bucket specified in command-line parameter.

            // Upload CloudFormation template to S3 bucket.

            // Create or update CloudFormation stack.  Stack name provided by command-line parameter.

            // Poll for stack creation/update status.
        }
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
}
