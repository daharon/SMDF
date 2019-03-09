/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.common.*
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.events.NotificationEvent
import us.aharon.monitoring.core.extensions.DynamoDBTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension
import us.aharon.monitoring.core.extensions.NotificationSqsQueueExtension
import us.aharon.monitoring.core.util.Env

import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class DatabaseStreamProcessorTest : KoinTest {

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class),
        ExtendWith(NotificationSqsQueueExtension::class))
    inner class CheckResultEvents {

        /**
         * A [CheckResultRecord] insertion with a CRITICAL status should be
         * routed to the [CheckResultProcessor], which will result in a
         * notification event being triggered.
         */
        @Test
        fun `Check result triggers a notification`() {
            val env: Env by inject()
            val json: ObjectMapper by inject()
            val sqs: AmazonSQS by inject()
            val checkResultEvent = DynamodbTestEvent(mapOf(
                    StreamRecord().withNewImage(
                            mapOf<String, AttributeValue>(
                                    "pk" to AttributeValue(CheckResultRecord.generateResultId("test", "test-check", "server-1.example.com")),
                                    "sk" to AttributeValue("2018-08-23T11:41:44Z"),  // completedAt
                                    "data" to AttributeValue("CHECK_RESULT"),
                                    "executedAt" to AttributeValue("2018-08-23T11:40:44Z"),
                                    "scheduledAt" to AttributeValue("2018-08-23T11:39:44Z"),
                                    "group" to AttributeValue("test"),
                                    "name" to AttributeValue("test-check"),
                                    "source" to AttributeValue("server-1.example.com"),
                                    // Critical status so that its routed to the notification handler.
                                    "status" to AttributeValue(CheckResultStatus.CRITICAL.name),
                                    "output" to AttributeValue("OK: This check is A-OK")
                            )
                    ) to OperationType.INSERT
            ))
            val testChecks = listOf(checks("test") {
                check("test-check") {
                    command = "true"
                    tags = listOf("linux")
                    handlers = listOf(TestNotificationHandler::class)
                    notification = "CPU usage is high"
                    contacts = listOf("devops")
                }
            })

            DatabaseStreamProcessor().run(checkResultEvent, testChecks)
            // Verify that the event was routed to the CheckResultProcessor and
            // triggered a notification.
            val request = ReceiveMessageRequest(env.get("NOTIFICATION_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val message = sqs.receiveMessage(request).messages.firstOrNull()
            assertNotNull(message)
            val parsedMsg = json.readValue<NotificationEvent>(message.body)
            assertEquals(TestNotificationHandler::class.qualifiedName, parsedMsg.handler)
        }
    }

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class ClientEvents {

        @Test
        fun `Client record deleted`() {
            val sqs: AmazonSQS by inject()
            // Create client queue.
            val queue = sqs.createQueue("TEST")
            // The event should be routed to the ClientCleanup function.
            val clientEvent = DynamodbTestEvent(mapOf(
                    StreamRecord().withOldImage(
                            mapOf<String, AttributeValue>(
                                    "pk" to AttributeValue("server-1.example.com"),
                                    "sk" to AttributeValue("2018-08-23T11:41:44Z"),  // createdAt
                                    "data" to AttributeValue(ClientRecord.DATA_FIELD),
                                    "tags" to AttributeValue().withL(AttributeValue("fake")),
                                    "queueArn" to AttributeValue(FAKE_SQS_QUEUE_ARN),
                                    "queueUrl" to AttributeValue(queue.queueUrl),
                                    "subscriptionArn" to AttributeValue(FAKE_SNS_SUBSCRIPTION_ARN),
                                    "active" to AttributeValue("true")
                            )
                    ) to OperationType.REMOVE
            ))

            DatabaseStreamProcessor().run(clientEvent, emptyList())
            // Verify that the client queue has been deleted.
            val listQueues = sqs.listQueues()
            assert(listQueues.queueUrls.isEmpty())
        }
    }
}
