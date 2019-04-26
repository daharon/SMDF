/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.smdf.core.checks.CheckGroup
import us.aharon.smdf.core.checks.getCheck
import us.aharon.smdf.core.checks.notificationHandlerParameterPath
import us.aharon.smdf.core.db.Dao
import us.aharon.smdf.core.events.NotificationEvent
import us.aharon.smdf.core.handlers.NotificationHandler
import us.aharon.smdf.core.util.Env


/**
 * Run the notification handler for the given notification.
 *
 * - Instantiate the given notification handler.
 * - Run the event handler.
 */
internal class NotificationProcessor :
        KoinComponent,
        AssumeRoleable by AssumeRole() {

    private val env: Env by inject()
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val db: Dao by inject()


    /**
     * For each event, run the specified handler.
     */
    fun run(event: SQSEvent, checks: List<CheckGroup>, context: Context) = event.records.forEach { message ->
        val notification = json.readValue<NotificationEvent>(message.body)
        val check = checks.getCheck(notification.checkResult?.group!!, notification.checkResult.name!!)
        log.debug { "Check result ID:  ${notification.checkResult.resultId}" }
        log.debug { "Check result completed at:  ${notification.checkResult.completedAt}" }
        log.info("Running notification handler:  ${notification.handler}")
        // Create an instance of the handler given its fully qualified name.
        val handler = Class.forName(notification.handler).newInstance() as NotificationHandler

        val credentialsProvider = getCredentials(
                "${notificationHandlerParameterPath(env.get("ENVIRONMENT"))}/${handler::class.qualifiedName}",
                "${handler::class.java.simpleName}-${env.get("ENVIRONMENT")}")

        // Execute the handler.
        try {
            handler.run(check, notification.checkResult!!, context, credentialsProvider)
        } catch (e: Exception) {
            val msg = "Notification handler failed:  ${e.message}"
            log.error(msg)
            db.saveNotification(handler, notification.checkResult, msg)
            throw e
        }
        db.saveNotification(handler, notification.checkResult, "Executed notification.")
    }
}
