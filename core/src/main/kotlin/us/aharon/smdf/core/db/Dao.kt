/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.smdf.core.handlers.NotificationHandler


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
                description = description)
        // Performed with two consecutive saves, since it's possible that both
        // records can have the same partition and range key, which will cause
        // a failure when using batchSave().
        db.save(client)
        db.save(clientHistoryEntry)
    }

    fun saveNotification(handler: NotificationHandler, result: CheckResultRecord, description: String) {
        val notification = NotificationRecord(
                handler = handler::class.java.canonicalName,
                checkGroup = result.group,
                checkName = result.name,
                source = result.source,
                resultId = result.resultId,
                resultCompletedAt = result.completedAt,
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
