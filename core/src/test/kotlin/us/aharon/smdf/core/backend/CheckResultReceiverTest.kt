/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.common.SQSTestEvent
import us.aharon.smdf.core.db.CheckResultRecord
import us.aharon.smdf.core.db.CheckResultStatus
import us.aharon.smdf.core.extensions.DynamoDBTableExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension

import java.time.ZonedDateTime
import kotlin.test.assertEquals


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(DynamoDBTableExtension::class))
class CheckResultReceiverTest : KoinTest {

    private val db: DynamoDBMapper by inject()
    private val singleTestEvent = SQSTestEvent(listOf(
            mapOf(
                    "scheduledAt" to "2018-08-23T11:37:44Z",
                    "executedAt" to "2018-08-23T11:38:00Z",
                    "completedAt" to "2018-08-23T11:39:00Z",
                    "group" to "test",
                    "name" to "simple_check",
                    "source" to "test-client-1.example.com",
                    "status" to CheckResultStatus.OK.name,
                    "output" to "OK: This check is A-OK"
            )
    ))
    private val multipleTestEvents = SQSTestEvent(listOf(
            mapOf(
                    "scheduledAt" to "2018-08-23T11:37:44Z",
                    "executedAt" to "2018-08-23T11:38:00Z",
                    "completedAt" to "2018-08-23T11:39:00Z",
                    "group" to "test",
                    "name" to "ok_check",
                    "source" to "test-client-1.example.com",
                    "status" to CheckResultStatus.OK.name,
                    "output" to "OK: This check is A-OK."
            ),
            mapOf(
                    "scheduledAt" to "2018-08-23T11:37:44Z",
                    "executedAt" to "2018-08-23T11:38:00Z",
                    "completedAt" to "2018-08-23T11:39:00Z",
                    "group" to "test",
                    "name" to "warning_check",
                    "source" to "test-client-1.example.com",
                    "status" to CheckResultStatus.WARNING.name,
                    "output" to "WARNING: We're heading for a bad time."
            ),
            mapOf(
                    "scheduledAt" to "2018-08-23T11:37:44Z",
                    "executedAt" to "2018-08-23T11:38:00Z",
                    "completedAt" to "2018-08-23T11:39:00Z",
                    "group" to "test",
                    "name" to "critical_check",
                    "source" to "test-client-1.example.com",
                    "status" to CheckResultStatus.CRITICAL.name,
                    "output" to "CRITICAL: This check failed."
            ),
            mapOf(
                    "scheduledAt" to "2018-08-23T11:37:44Z",
                    "executedAt" to "2018-08-23T11:38:00Z",
                    "completedAt" to "2018-08-23T11:39:00Z",
                    "group" to "test",
                    "name" to "unknown_check",
                    "source" to "test-client-1.example.com",
                    "status" to CheckResultStatus.UNKNOWN.name,
                    "output" to "UNKNOWN: This check did not run."
            )
    ))


    @Test
    fun `Receive a single check result`() {
        // Run the Lambda handler.
        CheckResultReceiver().run(singleTestEvent)

        // Query the database to verify that the expected data is saved.
        val query = DynamoDBQueryExpression<CheckResultRecord>()
                .withKeyConditionExpression("#pk = :resultId and #sk = :completedAt")
                .withExpressionAttributeValues(mapOf(
                        ":resultId" to AttributeValue(CheckResultRecord.generateResultId(
                                "test", "simple_check", "test-client-1.example.com")),
                        ":completedAt" to AttributeValue("2018-08-23T11:39:00Z")
                ))
                .withExpressionAttributeNames(mapOf("#pk" to "pk", "#sk" to "sk"))
        val results = db.query(CheckResultRecord::class.java, query)

        assertEquals(1, results.size)
        with(results.first()) {
            assertEquals("test", this.group)
            assertEquals("simple_check", this.name)
            assertEquals("test-client-1.example.com", this.source)
            assertEquals(ZonedDateTime.parse("2018-08-23T11:39:00Z"), this.completedAt)
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

        assertEquals(4, results.size)
    }
}
