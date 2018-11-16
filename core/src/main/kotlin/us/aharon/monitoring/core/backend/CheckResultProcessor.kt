/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject


/**
 * Process the check results as provided by the DynamoDB check results table stream.
 *
 * - Determine if forwarding to the notification handler is necessary.
 * - Was there a state change?
 * - Flapping detection?
 */
internal class CheckResultProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()


    fun run(event: DynamodbEvent) = event.records.forEach {
        // TODO:  Minimum viable functionality.
        // Was there a state change?
        // For each event, read from the database the previous event.
        // If there was a state change, send to notification handler queue.
        when (OperationType.fromValue(it.eventName)) {
            // MODIFY should not happen.  Ignore.
            OperationType.MODIFY -> log.info("Received MODIFY event.")
            // REMOVE - Ignore.
            OperationType.REMOVE -> log.info("Received REMOVE event.")
            // Handle INSERT.
            OperationType.INSERT -> log.info("Received INSERT event.")
            else -> log.error("Unknown event name provided:  ${it.eventName}")
        }
    }
}
