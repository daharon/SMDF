/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.checks.getCheck
import us.aharon.monitoring.core.db.Dao
import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.handlers.NotificationHandler


/**
 * Run the notification handler for the given notification.
 *
 * - Instantiate the given notification handler.
 * - Run the event handler.
 */
internal class NotificationProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val db: Dao by inject()


    /**
     * For each event, run the specified handler.
     */
    fun run(event: SQSEvent, checks: List<CheckGroup>, context: Context) = event.records.forEach { message ->
        val notification = json.readValue<NotificationEvent>(message.body)
        val check = checks.getCheck(notification.checkResult?.group!!, notification.checkResult.name!!)
        log.info("Running notification handler:  ${notification.handler}")
        // Create an instance of the handler given its fully qualified name.
        val handler = Class.forName(notification.handler).newInstance() as NotificationHandler
        // TODO: Implement AssumeRole operation to grant necessary user-defined access to the notification handler. Similar to [ServerlessExecutor].
        // Execute the handler.
        try {
            handler.execute(check, notification.checkResult!!, context)
        } catch (e: Exception) {
            db.saveNotification(handler, notification.checkResult.resultId!!, notification.checkResult.completedAt!!,
                    "Notification handler failed:  ${e.message}")
            throw e
        }
        db.saveNotification(handler, notification.checkResult.resultId!!, notification.checkResult.completedAt!!,
                "Executed notification.")
    }
}
