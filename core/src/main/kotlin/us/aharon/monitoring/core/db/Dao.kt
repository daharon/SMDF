/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject


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

    fun saveClient(client: ClientRecord) = db.save(client)

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
