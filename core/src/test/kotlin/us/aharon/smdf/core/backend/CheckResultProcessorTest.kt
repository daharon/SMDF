/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.test.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.api.checks
import us.aharon.smdf.core.api.check
import us.aharon.smdf.core.common.DynamodbTestEvent
import us.aharon.smdf.core.common.TestNotificationHandler
import us.aharon.smdf.core.db.CheckResultRecord
import us.aharon.smdf.core.db.CheckResultStatus
import us.aharon.smdf.core.extensions.DynamoDBTableExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension
import us.aharon.smdf.core.extensions.NotificationSqsQueueExtension
import us.aharon.smdf.core.util.Env

import java.time.ZonedDateTime
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class CheckResultProcessorTest {

    private val testChecks = listOf(checks("test") {
        check("test-check") {
            command = "true"
            tags = listOf("linux")
            handlers = listOf(TestNotificationHandler::class)
            notification = "CPU usage is high"
            additional = mapOf(
                    "extra metadata" to "be recorded along with the check's other data."
            )
            contacts = listOf("devops")
        }
    })
    private val okTestEvent = DynamodbTestEvent(mapOf(
            StreamRecord().withNewImage(
                    mapOf<String, AttributeValue>(
                            "pk" to AttributeValue(CheckResultRecord.generateResultId("test", "test-check", "server-1.example.com")),
                            "sk" to AttributeValue("2018-08-23T11:41:00Z"),  // completedAt
                            "data" to AttributeValue("CHECK_RESULT"),
                            "executedAt" to AttributeValue("2018-08-23T11:40:00Z"),
                            "scheduledAt" to AttributeValue("2018-08-23T11:39:00Z"),
                            "group" to AttributeValue("test"),
                            "name" to AttributeValue("test-check"),
                            "source" to AttributeValue("server-1.example.com"),
                            "status" to AttributeValue(CheckResultStatus.OK.name),
                            "output" to AttributeValue("OK: This check is A-OK")
                    )
            ) to OperationType.INSERT
    ))
    private val criticalTestEvent = DynamodbTestEvent(mapOf(
            StreamRecord().withNewImage(
                    mapOf<String, AttributeValue>(
                            "pk" to AttributeValue(CheckResultRecord.generateResultId("test", "test-check", "server-1.example.com")),
                            "sk" to AttributeValue("2018-08-23T11:41:00Z"),  // completedAt
                            "data" to AttributeValue("CHECK_RESULT"),
                            "executedAt" to AttributeValue("2018-08-23T11:40:00Z"),
                            "scheduledAt" to AttributeValue("2018-08-23T11:39:00Z"),
                            "group" to AttributeValue("test"),
                            "name" to AttributeValue("test-check"),
                            "source" to AttributeValue("server-1.example.com"),
                            "status" to AttributeValue(CheckResultStatus.CRITICAL.name),
                            "output" to AttributeValue("CRITICAL: This check is CRITICAL")
                    )
            ) to OperationType.INSERT
    ))


    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(NotificationSqsQueueExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class NoNotificationSent : KoinTest {

        private val env: Env by inject()
        private val sqs: AmazonSQS by inject()
        private val db: DynamoDBMapper by inject()

        @Test
        fun `Single OK check result does not send notification`() {
            CheckResultProcessor().run(okTestEvent.records.first(), testChecks)

            val request = ReceiveMessageRequest(env.get("NOTIFICATION_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val notificationMessage = sqs.receiveMessage(request).messages.firstOrNull()
            // No message received because no message sent.
            assertNull(notificationMessage)
        }

        /**
         * Pre-insert a CRITICAL check result and then run the check result processor
         * on a second CRITICAL check result.  Since there is no state change there
         * should not be a notification sent.
         */
        @Test
        fun `Second CRITICAL check result does not send notification`() {
            // Initial CRITICAL check result.
            val initialCheckResult = CheckResultRecord(
                    completedAt = ZonedDateTime.parse("2018-08-23T11:32:00Z"),
                    scheduledAt = ZonedDateTime.parse("2018-08-23T11:30:00Z"),
                    executedAt = ZonedDateTime.parse("2018-08-23T11:31:00Z"),
                    group = "test",
                    name = "test-check",
                    source = "server-1.example.com",
                    status = CheckResultStatus.CRITICAL,
                    output = "")
            db.save(initialCheckResult)

            // Process second CRITICAL check result.
            CheckResultProcessor().run(criticalTestEvent.records.first(), testChecks)

            val request = ReceiveMessageRequest(env.get("NOTIFICATION_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val notificationMessage = sqs.receiveMessage(request).messages.firstOrNull()
            // No message received because no message sent.
            assertNull(notificationMessage)
        }
    }

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(NotificationSqsQueueExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class NotificationSent : KoinTest {

        private val env: Env by inject()
        private val sqs: AmazonSQS by inject()
        private val db: DynamoDBMapper by inject()

        /**
         * Critical check result received with no previous check results should
         * send a notification.
         */
        @Test
        fun `Single CRITICAL check result sends notification`() {
            CheckResultProcessor().run(criticalTestEvent.records.first(), testChecks)

            val request = ReceiveMessageRequest(env.get("NOTIFICATION_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val notificationMessage = sqs.receiveMessage(request).messages.firstOrNull()
            assertNotNull(notificationMessage)
        }

        /**
         * Pre-insert an OK check result and then run the check result processor
         * on a CRITICAL check result.  Since there is a state change there
         * should be a notification sent.
         */
        @Test
        fun `CRITICAL check result after OK sends notification`() {
            // Initial OK check result.
            val initialCheckResult = CheckResultRecord(
                    completedAt = ZonedDateTime.parse("2018-08-23T11:32:00Z"),
                    scheduledAt = ZonedDateTime.parse("2018-08-23T11:30:00Z"),
                    executedAt = ZonedDateTime.parse("2018-08-23T11:31:00Z"),
                    group = "test",
                    name = "test-check",
                    source = "server-1.example.com",
                    status = CheckResultStatus.OK,
                    output = "")
            db.save(initialCheckResult)

            // Process second CRITICAL check result.
            CheckResultProcessor().run(criticalTestEvent.records.first(), testChecks)

            val request = ReceiveMessageRequest(env.get("NOTIFICATION_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val notificationMessage = sqs.receiveMessage(request).messages.firstOrNull()
            assertNotNull(notificationMessage)
        }
    }
}
