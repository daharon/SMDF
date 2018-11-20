/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.db.ZonedDateTimeConverter


/**
 * Process the check results as provided by the DynamoDB check results table stream.
 *
 * - Determine if forwarding to the notification handler is necessary.
 * - Was there a state change?
 * - Flapping detection?
 */
internal class CheckResultProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val db: DynamoDBMapper by inject()


    /**
     * For each event, read from the database the previous event.
     * If there was a state change, send to the notification handler queue.
     */
    fun run(event: DynamodbEvent) = event.records.forEach {
        // TODO:  Minimum viable functionality.
        when (OperationType.fromValue(it.eventName)) {
            OperationType.INSERT -> {
                log.info("Received INSERT event.")
                val newCheckResultRecord = db.marshallIntoObject(CheckResultRecord::class.java, it.dynamodb.newImage)
                val previousCheckResultRecord = previousRecord(newCheckResultRecord)
                log.info("Previous record:  $previousCheckResultRecord")
                if (previousCheckResultRecord == null) {
                    // This is the first time this check has run on the client.
                    if (newCheckResultRecord.status != CheckResultStatus.OK) {
                        sendToNotificationHandler(null, newCheckResultRecord)
                    }
                } else {
                    log.info("Previous status: ${previousCheckResultRecord.status}, New status: ${newCheckResultRecord.status}")
                    if (previousCheckResultRecord.status != newCheckResultRecord.status) {
                        sendToNotificationHandler(null, newCheckResultRecord)
                    }
                }
            }
            OperationType.MODIFY -> log.info("Ignoring MODIFY event.")
            OperationType.REMOVE -> log.info("Ignoring REMOVE event.")
            else -> log.error("Unknown event name provided:  ${it.eventName}")
        }
    }

    /**
     * Query the database for the previous record and return its status.
     */
    private fun previousRecord(newRecord: CheckResultRecord): CheckResultRecord? {
        val query = DynamoDBQueryExpression<CheckResultRecord>()
                .withKeyConditionExpression("#id = :id AND #timestamp < :timestamp")
                .withExpressionAttributeValues(mapOf(
                        ":id" to AttributeValue(newRecord.id),
                        ":timestamp" to AttributeValue(ZonedDateTimeConverter().convert(newRecord.timestamp!!))))
                .withExpressionAttributeNames(mapOf("#id" to "id", "#timestamp" to "timestamp"))
                .withScanIndexForward(false)
                .withLimit(1)
        val result = db.query(CheckResultRecord::class.java, query)
        return result.firstOrNull()
    }

    /**
     * State change requires a notification.
     */
    private fun sendToNotificationHandler(old: CheckResultRecord?, new: CheckResultRecord) {
        // TODO:  Send message to the notification handler queue.
        log.info("State change from ${old?.status} to ${new.status}.")
    }
}
