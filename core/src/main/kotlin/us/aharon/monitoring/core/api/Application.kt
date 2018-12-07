/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.log.Logger.SLF4JLogger
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject
import picocli.CommandLine

import us.aharon.monitoring.core.backend.*
import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.cli.Base
import us.aharon.monitoring.core.di.modules
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.http.ClientRegistrationHandler
import us.aharon.monitoring.core.http.ClientRegistrationRequest
import us.aharon.monitoring.core.http.ClientRegistrationResponse
import us.aharon.monitoring.core.mutators.Mutator


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

    protected val log: KLogger by inject { parametersOf(this::class.java.simpleName) }

    private val _clientRegistration by lazy { ClientRegistrationHandler() }
    private val _checkScheduler by lazy { CheckScheduler() }
    private val _clientCleanup by lazy { ClientCleanup() }
    private val _checkResultReceiver by lazy { CheckResultReceiver() }
    private val _checkResultProcessor by lazy { CheckResultProcessor() }
    private val _notificationProcessor by lazy { NotificationProcessor() }
    private val _serverlessCheckProcessor by lazy { ServerlessCheckProcessor() }


    init {
        startKoin(listOf(modules), logger = SLF4JLogger())
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
     *   re-create the queue and subscription.  The [us.aharon.monitoring.core.backend.ClientCleanup] handler will
     *   deal with any cleanup that is necessary.
     */
    fun clientRegistration(event: ClientRegistrationRequest, context: Context): ClientRegistrationResponse {
        log.info("Received event:  $event")
        return this._clientRegistration.run(event)
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
        this._checkScheduler.run(event.time, this.checks)
    }

    /**
     * Receive scheduled check results and save to database.
     */
    fun checkResultReceiver(event: SQSEvent, context: Context) {
        log.info("Received ${event.records.size} SQS messages.")
        event.records.forEach { log.info("Message body:  ${it.body}") }
        this._checkResultReceiver.run(event)
    }

    /**
     * Process the check results as provided by the DynamoDB stream.
     *
     * - Determine if forwarding to the notification handler is necessary.
     * - Was there a state change?
     * - Flapping detection?
     */
    fun checkResultProcessor(event: DynamodbEvent, context: Context) {
        log.info("DynamoDB stream event: $event")
        this._checkResultProcessor.run(event, checks)
    }

    /**
     * Run the specified notification handler given the metadata passed along with the event.
     *
     * - Apply the application defined Filters and Mutators before forwarding to the notification handler.
     */
    fun notificationProcessor(event: SQSEvent, context: Context) {
        event.records.forEach { log.info("Message body:  ${it.body}") }
        this._notificationProcessor.run(event, checks, context)
    }

    /**
     * Cleanup client resources (queues, SNS subscriptions) when modified or
     * deleted in the clients DynamoDB table.
     */
    fun clientCleanup(event: DynamodbEvent, context: Context) {
        log.info("DynamoDB stream event: $event")
        this._clientCleanup.run(event)
    }

    /**
     * Run the specified serverless check.
     */
    fun serverlessCheckProcessor(event: SQSEvent, context: Context) {
        event.records.forEach { log.info("Message body:  ${it.body}") }
        this._serverlessCheckProcessor.run(event, checks, context)
    }
}
