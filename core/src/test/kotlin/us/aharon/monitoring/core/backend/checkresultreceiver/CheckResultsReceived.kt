/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkresultreceiver

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.CheckResultReceiver
import us.aharon.monitoring.core.common.SQSTestEvent
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus

import java.time.ZonedDateTime
import kotlin.test.assertEquals


@ExtendWith(LocalstackExtension::class)
class CheckResultsReceived : BaseTest() {

    private val db: DynamoDBMapper by inject()
    private val singleTestEvent = SQSTestEvent(listOf(
            mapOf(
                    "name" to "server-1.example.com",
                    "timestamp" to "2018-08-23T11:39:44Z",
                    "status" to CheckResultStatus.OK.name,
                    "output" to "OK: This check is A-OK"
            )
    ))
    private val multipleTestEvents = SQSTestEvent(listOf(
            mapOf(
                    "name" to "server-1.example.com",
                    "timestamp" to "2018-08-23T11:39:44Z",
                    "status" to CheckResultStatus.OK.name,
                    "output" to "OK: This check is A-OK."
            ),
            mapOf(
                    "name" to "server-3.example.com",
                    "timestamp" to "2018-08-23T11:43:00Z",
                    "status" to CheckResultStatus.WARNING.name,
                    "output" to "WARNING: We're heading for a bad time."
            ),
            mapOf(
                    "name" to "server-2.example.com",
                    "timestamp" to "2018-08-23T11:41:00Z",
                    "status" to CheckResultStatus.CRITICAL.name,
                    "output" to "CRITICAL: This check failed."
            )
    ))


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
    fun `Receive a single check result`() {
        // Run the Lambda handler.
        CheckResultReceiver().run(singleTestEvent)

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

    @Test
    fun `Receive multiple check results`() {
        // Run the Lambda handler.
        CheckResultReceiver().run(multipleTestEvents)

        // Scan the database to verify that the events were saved.
        val scan = DynamoDBScanExpression()
        val results = db.scan(CheckResultRecord::class.java, scan)

        assertEquals(3, results.size)
    }
}
