/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkResultStatus

import cloud.localstack.LocalstackExtension
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.CheckResultReceiver
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus

import java.time.ZonedDateTime
import kotlin.test.assertEquals


@ExtendWith(LocalstackExtension::class)
class SingleCheckResultStatus : BaseTest() {

    private val json: ObjectMapper by inject()
    private val db: DynamoDBMapper by inject()
    private val testEvent = SQSEvent().apply {
        records = listOf(
                SQSEvent.SQSMessage().apply {
                    messageId = "19dd0b57-b21e-4ac1-bd88-01bbb068cb78"
                    receiptHandle = "MessageReceiptHandle"
                    attributes = mapOf(
                            "ApproximateReceiveCount" to "1",
                            "SentTimestamp" to "1523232000000",
                            "SenderId" to "123456789012",
                            "ApproximateFirstReceiveTimestamp" to "1523232000001"
                    )
                    messageAttributes = emptyMap()
                    md5OfBody = "7b270e59b47ff90a553787216d55d91d"
                    eventSource = "aws:sqs"
                    eventSourceArn = "arn:aws:sqs:us-east-1:123456789012:CheckResultsQueue"
                    awsRegion = Regions.US_EAST_1.name
                    body = json.writeValueAsString(mapOf<String, String>(
                            "name" to "server-1.example.com",
                            "timestamp" to "2018-08-23T11:39:44Z",
                            "status" to CheckResultStatus.OK.name,
                            "output" to "OK: This check is A-OK"
                    ))
                }
        )
    }


    @BeforeEach
    fun createDynamoDBTable() {
        val client: AmazonDynamoDB by inject()
        val createTableRequest = db.generateCreateTableRequest(CheckResultRecord::class.java)
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))
        client.createTable(createTableRequest)
    }

    @AfterEach
    fun deleteDynamoDBTable() {
        val client: AmazonDynamoDB by inject()
        val deleteTableRequest = db.generateDeleteTableRequest(CheckResultRecord::class.java)
        client.deleteTable(deleteTableRequest)
    }

    @Test
    fun `Process a single check result`() {
        // Run the Lambda handler.
        CheckResultReceiver().run(testEvent)

        // Query the database to verify that the expected data is saved.
        val query = DynamoDBQueryExpression<CheckResultRecord>()
                .withKeyConditionExpression("#name = :name and #timestamp = :timestamp")
                .withExpressionAttributeValues(mapOf(
                        ":name" to AttributeValue("server-1.example.com"),
                        ":timestamp" to AttributeValue("2018-08-23T11:39:44Z")
                ))
                .withExpressionAttributeNames(mapOf(
                        "#name" to "name",
                        "#timestamp" to "timestamp"
                ))
        val results = db.query(CheckResultRecord::class.java, query)

        assertEquals(1, results.size)
        with(results.first()) {
            assertEquals("server-1.example.com", this.name)
            assertEquals(ZonedDateTime.parse("2018-08-23T11:39:44Z"), this.timestamp)
            assertEquals(CheckResultStatus.OK, this.status)
            assertEquals("OK: This check is A-OK", this.output)
        }
    }
}
