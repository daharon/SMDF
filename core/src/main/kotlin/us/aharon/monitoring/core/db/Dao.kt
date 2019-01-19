/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.handlers.NotificationHandler

import java.time.ZonedDateTime


internal class Dao : KoinComponent {

    private val db: DynamoDBMapper by inject()


    fun getClient(name: String): ClientRecord? {
        val query = DynamoDBQueryExpression<ClientRecord>()
                .withKeyConditionExpression("#pk = :clientId AND #data = :data")
                .withExpressionAttributeNames(mapOf("#pk" to "pk", "#data" to "data"))
                .withExpressionAttributeValues(mapOf(
                        ":clientId" to AttributeValue(name),
                        ":data" to AttributeValue(ClientRecord.DATA_FIELD)))
                .withConsistentRead(true)
                .withIndexName(PK_DATA_INDEX)
                .withLimit(1)
        val result = db.query(ClientRecord::class.java, query)
        return result.firstOrNull()
    }

    fun saveClient(client: ClientRecord, description: String = "Client updated.") {
        val clientHistoryEntry = ClientHistoryRecord(
                name = client.name,
                timestamp = ZonedDateTime.now(),
                description = description)
        db.batchSave(client, clientHistoryEntry)
    }

    fun saveNotification(handler: NotificationHandler, resultId: String, resultCompletedAt: ZonedDateTime,
                         description: String) {
        val notification = NotificationRecord(
                handler = handler::class.java.canonicalName,
                resultId = resultId,
                resultCompletedAt = resultCompletedAt,
                description = description)
        db.save(notification)
    }

    fun marshallClientRecord(itemAttributes: Map<String, AttributeValue>): ClientRecord =
            db.marshallIntoObject(ClientRecord::class.java, itemAttributes)

    fun marshallCheckResultRecord(itemAttributes: Map<String, AttributeValue>): CheckResultRecord =
            db.marshallIntoObject(CheckResultRecord::class.java, itemAttributes)

    fun previousCheckResultRecord(checkResult: CheckResultRecord): CheckResultRecord? {
        val query = DynamoDBQueryExpression<CheckResultRecord>()
                .withKeyConditionExpression("#pk = :resultId AND #sk < :completedAt")
                .withExpressionAttributeNames(mapOf("#pk" to "pk", "#sk" to "sk"))
                .withExpressionAttributeValues(mapOf(
                        ":resultId" to AttributeValue(checkResult.resultId),
                        ":completedAt" to AttributeValue(ZonedDateTimeConverter().convert(checkResult.completedAt!!))))
                .withScanIndexForward(false)
                .withLimit(1)
        val result = db.query(CheckResultRecord::class.java, query)
        return result.firstOrNull()
    }
}
