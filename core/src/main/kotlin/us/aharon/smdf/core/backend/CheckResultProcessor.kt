/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.core.KoinComponent
import org.koin.core.inject

import us.aharon.smdf.core.checks.CheckGroup
import us.aharon.smdf.core.checks.getCheck
import us.aharon.smdf.core.db.CheckResultRecord
import us.aharon.smdf.core.db.CheckResultStatus
import us.aharon.smdf.core.db.Dao
import us.aharon.smdf.core.events.NotificationEvent
import us.aharon.smdf.core.handlers.NotificationHandler
import us.aharon.smdf.core.util.Env

import kotlin.reflect.KClass


/**
 * Process the check results as provided by the DynamoDB check results table stream.
 *
 * - Determine if forwarding to the notification handler is necessary.
 * - Was there a state change?
 * - Flapping detection?
 */
internal class CheckResultProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val env: Env by inject()
    private val json: ObjectMapper by inject()
    private val db: Dao by inject()
    private val sqs: AmazonSQS by inject()

    private val NOTIFICATION_QUEUE: String by lazy { env.get("NOTIFICATION_QUEUE") }


    /**
     * For each event, read from the database the previous event.
     * If there was a state change, send to the notification handler queue.
     */
    fun run(record: DynamodbEvent.DynamodbStreamRecord, checks: List<CheckGroup>) {
        when (OperationType.fromValue(record.eventName)) {
            OperationType.INSERT -> {
                log.debug { "Received INSERT event." }
                val newCheckResultRecord = db.marshallCheckResultRecord(record.dynamodb.newImage)
                val previousCheckResultRecord = db.previousCheckResultRecord(newCheckResultRecord)
                val handlers = checks.getCheck(newCheckResultRecord.group!!, newCheckResultRecord.name!!).handlers
                log.debug { "Previous record:  $previousCheckResultRecord" }

                when (previousCheckResultRecord) {
                    null -> {
                        // This is the first time this check has run on the client.
                        if (newCheckResultRecord.status != CheckResultStatus.OK) {
                            sendToNotificationHandler(null, newCheckResultRecord, handlers)
                        }
                    }
                    else -> {
                        log.debug { "Previous status: ${previousCheckResultRecord.status}, New status: ${newCheckResultRecord.status}" }
                        if (previousCheckResultRecord.status != newCheckResultRecord.status) {
                            sendToNotificationHandler(previousCheckResultRecord, newCheckResultRecord, handlers)
                        }
                    }
                }
            }
            OperationType.MODIFY -> log.debug { "Ignoring MODIFY event." }
            OperationType.REMOVE -> log.debug { "Ignoring REMOVE event." }
            else -> log.error("Unknown event name provided:  ${record.eventName}")
        }
    }

    /**
     * State change requires a notification.
     */
    private fun sendToNotificationHandler(old: CheckResultRecord?, new: CheckResultRecord, handlers: List<KClass<out NotificationHandler>>) {
        log.info("Source: ${new.source}, Group: ${new.group}, Check: ${new.name}")
        log.info("State change from ${old?.status ?: "N/A"} to ${new.status}.")
        handlers.forEach { handler ->
            val notificationEvent = NotificationEvent(
                    handler = handler.qualifiedName!!,
                    checkResult = new
            )
            val message = json.writeValueAsString(notificationEvent)
            val req = SendMessageRequest()
                    .withQueueUrl(NOTIFICATION_QUEUE)
                    .withMessageBody(message)
            val result = sqs.sendMessage(req)
            log.debug { "Sent message to notification queue:  ${result.messageId}" }
        }
    }
}
