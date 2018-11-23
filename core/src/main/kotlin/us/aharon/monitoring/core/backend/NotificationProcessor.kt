/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject


/**
 * Run the notification handler for the given notification.
 *
 * - Instantiate the given notification handler.
 * - Run the event handler.
 */
internal class NotificationProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()


    /**
     * For each event, run the specified handler.
     */
    fun run(event: SQSEvent) = event.records.forEach { message ->
        // TODO:  Implement.
        log.info("TODO:  Run notification handler.")
    }
}
