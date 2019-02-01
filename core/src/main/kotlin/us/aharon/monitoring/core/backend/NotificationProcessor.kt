/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.checks.getCheck
import us.aharon.monitoring.core.checks.notificationHandlerParameterPath
import us.aharon.monitoring.core.db.Dao
import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.handlers.NotificationHandler
import us.aharon.monitoring.core.util.Env


/**
 * Run the notification handler for the given notification.
 *
 * - Instantiate the given notification handler.
 * - Run the event handler.
 */
internal class NotificationProcessor : KoinComponent {

    private val env: Env by inject()
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val db: Dao by inject()
    private val ssm: AWSSimpleSystemsManagement by inject()


    /**
     * For each event, run the specified handler.
     */
    fun run(event: SQSEvent, checks: List<CheckGroup>, context: Context) = event.records.forEach { message ->
        val notification = json.readValue<NotificationEvent>(message.body)
        val check = checks.getCheck(notification.checkResult?.group!!, notification.checkResult.name!!)
        log.info("Running notification handler:  ${notification.handler}")
        // Create an instance of the handler given its fully qualified name.
        val handler = Class.forName(notification.handler).newInstance() as NotificationHandler

        // Get the role ARN from the parameter store.
        val paramRequest = GetParameterRequest()
                .withName("${notificationHandlerParameterPath(env.get("ENVIRONMENT"))}/${handler::class.qualifiedName}")
        val notificationHandlerRoleArn = ssm.getParameter(paramRequest).parameter.value
        log.info { "Notification handler role ARN:  $notificationHandlerRoleArn" }

        // Create an IAM credentials provider that assumes the notification handler's role.
        val sessionName = "${handler::class.java.simpleName}-${env.get("ENVIRONMENT")}"
        val credentialsProvider = STSAssumeRoleSessionCredentialsProvider
                .Builder(notificationHandlerRoleArn, sessionName)
                .build()

        // Execute the handler.
        try {
            handler.run(check, notification.checkResult!!, context, credentialsProvider)
        } catch (e: Exception) {
            db.saveNotification(handler, notification.checkResult.resultId!!, notification.checkResult.completedAt!!,
                    "Notification handler failed:  ${e.message}")
            throw e
        }
        db.saveNotification(handler, notification.checkResult.resultId!!, notification.checkResult.completedAt!!,
                "Executed notification.")
    }
}
