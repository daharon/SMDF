/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
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

import us.aharon.smdf.core.api.checks
import us.aharon.smdf.core.api.check
import us.aharon.smdf.core.common.*
import us.aharon.smdf.core.db.*
import us.aharon.smdf.core.events.NotificationEvent
import us.aharon.smdf.core.extensions.DynamoDBTableExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension
import us.aharon.smdf.core.extensions.NotificationSqsQueueExtension
import us.aharon.smdf.core.util.Env

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

        @Test
        fun `Client de-activated`() {
            val sqs: AmazonSQS by inject()
            val db: DynamoDBMapper by inject()
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
                                    // Client _was_ active.
                                    "active" to AttributeValue("true")
                            )
                    ).withNewImage(
                            mapOf<String, AttributeValue>(
                                    "pk" to AttributeValue("server-1.example.com"),
                                    "sk" to AttributeValue("2018-08-23T11:41:44Z"),
                                    "data" to AttributeValue(ClientRecord.DATA_FIELD),
                                    "tags" to AttributeValue().withL(AttributeValue("fake")),
                                    "queueArn" to AttributeValue(FAKE_SQS_QUEUE_ARN),
                                    "queueUrl" to AttributeValue(queue.queueUrl),
                                    "subscriptionArn" to AttributeValue(FAKE_SNS_SUBSCRIPTION_ARN),
                                    // Client is now NOT active!
                                    "active" to AttributeValue("false")
                            )
                    ) to OperationType.MODIFY
            ))

            DatabaseStreamProcessor().run(clientEvent, emptyList())

            // Verify that the client queue has been deleted.
            val listQueues = sqs.listQueues()
            assert(listQueues.queueUrls.isEmpty())
            // Verify that the client history record was saved.
            val query = DynamoDBQueryExpression<ClientHistoryRecord>()
                    .withKeyConditionExpression("#pk = :clientName and #data = :data")
                    .withExpressionAttributeValues(mapOf(
                            ":clientName" to AttributeValue("server-1.example.com"),
                            ":data" to AttributeValue(ClientHistoryRecord.DATA_FIELD)
                    ))
                    .withExpressionAttributeNames(mapOf("#pk" to "pk", "#data" to "data"))
                    .withIndexName(PK_DATA_INDEX)
                    .withLimit(1)
            val clientHistory = db.query(ClientHistoryRecord::class.java, query).first()
            assert(clientHistory.description!!.contains("client queue and subscription deleted", true))
        }
    }

    @Nested
    @ExtendWith(LoadModulesExtension::class)
    inner class NotificationEvents {

        @Test
        fun `Notification records should be ignored`() {
            val notificationEvent = DynamodbTestEvent(mapOf(
                    StreamRecord().withNewImage(
                            mapOf<String, AttributeValue>(
                                    "pk" to AttributeValue("abc123"),
                                    "sk" to AttributeValue("2018-08-23T11:41:44Z"),
                                    "data" to AttributeValue(NotificationRecord.DATA_FIELD),
                                    "handler" to AttributeValue("com.example.GoNotify"),
                                    "checkGroup" to AttributeValue("test"),
                                    "checkName" to AttributeValue("Test Check"),
                                    "source" to AttributeValue("server-1.example.com"),
                                    "resultId" to AttributeValue("xyz789"),
                                    "resultCompletedAt" to AttributeValue("2018-08-23T11:40:00Z"),
                                    "description" to AttributeValue("Notification sent.")
                            )
                    ) to OperationType.INSERT
            ))
            // No resources available.
            // If the process attempts to interact with the DB or other resources,
            // then an exception should be thrown.
            DatabaseStreamProcessor().run(notificationEvent, emptyList())
        }
    }
}
