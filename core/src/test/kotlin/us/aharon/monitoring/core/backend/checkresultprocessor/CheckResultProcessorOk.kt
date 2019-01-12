/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkresultprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.lambda.runtime.Context
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.backend.CheckResultProcessor
import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.common.DynamodbTestEvent
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.extensions.CheckResultTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension
import us.aharon.monitoring.core.handlers.NotificationHandler


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(CheckResultTableExtension::class))
class CheckResultProcessorOk : KoinTest {

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
                            "pk" to AttributeValue(CheckResultRecord.generateResultId("test", "test-check", "server-1.example.com")),
                            "sk" to AttributeValue("2018-08-23T11:41:44Z"),  // completedAt
                            "data" to AttributeValue(CheckResultRecord.generateDataKey("test", "test-check", "server-1.example.com")),
                            "executedAt" to AttributeValue("2018-08-23T11:40:44Z"),
                            "scheduledAt" to AttributeValue("2018-08-23T11:39:44Z"),
                            "group" to AttributeValue("test"),
                            "name" to AttributeValue("test-check"),
                            "source" to AttributeValue("server-1.example.com"),
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


    @Test
    fun `Receive a single OK check result`() {
        // Run the Lambda handler.
        CheckResultProcessor().run(singleTestEvent.records.first(), testChecks)

        // TODO: Verify test results.
        // db.query()
    }
}
