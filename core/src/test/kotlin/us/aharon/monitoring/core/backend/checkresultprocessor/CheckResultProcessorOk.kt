/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkresultprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.lambda.runtime.Context
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.CheckResultProcessor
import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.common.DynamodbTestEvent
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.handlers.NotificationHandler


@ExtendWith(LocalstackExtension::class)
class CheckResultProcessorOk : BaseTest() {

    private val db: DynamoDBMapper by inject()
    private val testChecks = listOf(checks("test") {
        check("test-check") {
            command = "true"
            subscribers = listOf("linux")
            handlers = listOf(TestNotificationHandler::class)
            notification = "CPU usage is high"
            additional = mapOf(
                    "extra metadata" to "be recorded along with the check's other data."
            )
            contacts = listOf("devops")
        }
    })
    private val singleTestEvent = DynamodbTestEvent(mapOf(
            StreamRecord().withNewImage(
                    mapOf<String, AttributeValue>(
                            "group" to AttributeValue("test"),
                            "name" to AttributeValue("test-check"),
                            "source" to AttributeValue("server-1.example.com"),
                            "timestamp" to AttributeValue("2018-08-23T11:39:44Z"),
                            "status" to AttributeValue(CheckResultStatus.OK.name),
                            "output" to AttributeValue("OK: This check is A-OK")
                    )
            ) to OperationType.INSERT
    ))

    class TestNotificationHandler : NotificationHandler() {
        override val policies: List<String> = emptyList()

        override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context) {
            println("Running notification for:")
            println("Check:  $check")
            println("Check Result:  $checkResult")
        }
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
    fun `Receive a single OK check result`() {
        // Run the Lambda handler.
        CheckResultProcessor().run(singleTestEvent, testChecks)

        // TODO: Verify test results.
    }
}
