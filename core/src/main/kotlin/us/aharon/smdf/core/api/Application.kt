/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import mu.KLogger
import org.koin.Logger.SLF4JLogger
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.get
import org.koin.core.parameter.parametersOf
import picocli.CommandLine

import us.aharon.smdf.core.backend.*
import us.aharon.smdf.core.checks.CheckGroup
import us.aharon.smdf.core.cli.Base
import us.aharon.smdf.core.di.modules
import us.aharon.smdf.core.filters.Filter
import us.aharon.smdf.core.backend.messages.*
import us.aharon.smdf.core.mutators.Mutator


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
abstract class Application : KoinComponent {

    abstract val checks: List<CheckGroup>
    abstract val filters: List<Filter>
    abstract val mutators: List<Mutator>

    protected val log: KLogger

    private val _clientRegistration by lazy { ClientRegistration() }
    private val _clientDeregistration by lazy { ClientDeregistration() }
    private val _checkScheduler by lazy { CheckScheduler() }
    private val _databaseStreamProcessor by lazy { DatabaseStreamProcessor() }
    private val _checkResultReceiver by lazy { CheckResultReceiver() }
    private val _notificationProcessor by lazy { NotificationProcessor() }
    private val _serverlessCheckProcessor by lazy { ServerlessCheckProcessor() }


    init {
        startKoin {
            modules(modules)
            logger(SLF4JLogger())
        }
        log = get { parametersOf(this::class.simpleName) }
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
        CommandLine.run(Base(this), *args)
    }

    /**
     * Handler that registers a client for monitoring.
     *
     * - Verify that the client name is unique.
     * - Create SQS queue for the client subscriber with a filter based on the client's provided tags.
     * - Subscribe the SQS queue to the CLIENT_CHECK_TOPIC.
     * - If the client already exists but either the queue or the subscription is missing, then
     *   re-create the queue and subscription.  The [us.aharon.smdf.core.backend.ClientCleanup] handler will
     *   deal with any cleanup that is necessary.
     */
    @Suppress("UNUSED_PARAMETER")
    fun clientRegistration(event: ClientRegistrationRequest, context: Context): ClientRegistrationResponse {
        log.debug { "Received event:  $event" }
        return this._clientRegistration.run(event)
    }

    /**
     * Handler that marks a client as in-active.
     * Cleanup of the client's resources will be handled by [ClientCleanup].
     */
    @Suppress("UNUSED_PARAMETER")
    fun clientDeregistration(event: ClientDeregistrationRequest, context: Context): ClientDeregistrationResponse {
        log.debug { "Received event:  $event" }
        return _clientDeregistration.run(event)
    }

    /**
     * Fire events to the SNS Fanout Topic.
     *
     * This handler is triggered by the CloudWatch scheduled event resource.
     * Examine all defined checks and push those whose [interval] % [current_minute] equals zero.
     *
     * Requires the ARN of the SNS Check Fanout topic.
     */
    @Suppress("UNUSED_PARAMETER")
    fun checkScheduler(event: ScheduledEvent, context: Context) {
        log.debug { "Received event:  $event" }
        this._checkScheduler.run(event.time, this.checks)
    }

    /**
     * Receive scheduled check results and save to database.
     */
    @Suppress("UNUSED_PARAMETER")
    fun checkResultReceiver(event: SQSEvent, context: Context) {
        log.debug { "Received ${event.records.size} SQS messages." }
        if (log.isDebugEnabled) {
            event.records.forEach { log.debug("Message body:  ${it.body}") }
        }
        this._checkResultReceiver.run(event)
    }

    /**
     * Process the messages provided by the DynamoDB stream.
     *
     * - Process check results.
     * - Process client changes.
     */
    @Suppress("UNUSED_PARAMETER")
    fun databaseStreamProcessor(event: DynamodbEvent, context: Context) {
        log.debug { "DynamoDB stream event: $event" }
        this._databaseStreamProcessor.run(event, checks)
    }

    /**
     * Run the specified notification handler given the metadata passed along with the event.
     *
     * - Apply the application defined Filters and Mutators before forwarding to the notification handler.
     */
    fun notificationProcessor(event: SQSEvent, context: Context) {
        if (log.isDebugEnabled) {
            event.records.forEach { log.debug("Message body:  ${it.body}") }
        }
        this._notificationProcessor.run(event, checks, context)
    }

    /**
     * Run the specified serverless check.
     */
    fun serverlessCheckProcessor(event: SQSEvent, context: Context) {
        if (log.isDebugEnabled) {
            event.records.forEach { log.debug("Message body:  ${it.body}") }
        }
        this._serverlessCheckProcessor.run(event, checks, context)
    }
}
