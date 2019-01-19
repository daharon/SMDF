/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.databasestreamprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.backend.DatabaseStreamProcessor
import us.aharon.monitoring.core.common.*
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.extensions.DynamoDBTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension


class DatabaseStreamProcessorRouting {

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class CheckResultEvents {

        private val checkResultEvent = DynamodbTestEvent(mapOf(
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
                                "status" to AttributeValue(CheckResultStatus.OK.name),
                                "output" to AttributeValue("OK: This check is A-OK")
                        )
                ) to OperationType.INSERT
        ))
        private val testChecks = listOf(checks("test") {
            check("test-check") {
                command = "true"
                subscribers = listOf("linux")
                handlers = listOf(TestNotificationHandler::class)
                notification = "CPU usage is high"
                contacts = listOf("devops")
            }
        })

        @Test
        fun `Check result event routed properly`() {
            DatabaseStreamProcessor().run(checkResultEvent, testChecks)
            // TODO: Verify that the event was routed to the correct handler.
        }
    }

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class ClientEvents {

        private val clientEvent = DynamodbTestEvent(mapOf(
                StreamRecord().withOldImage(
                        mapOf<String, AttributeValue>(
                                "pk" to AttributeValue("server-1.example.com"),
                                "sk" to AttributeValue("2018-08-23T11:41:44Z"),  // createdAt
                                "data" to AttributeValue(ClientRecord.DATA_FIELD),
                                "tags" to AttributeValue().withL(AttributeValue("fake")),
                                "queueArn" to AttributeValue(FAKE_SQS_QUEUE_ARN),
                                "queueUrl" to AttributeValue(FAKE_SQS_QUEUE_URL),
                                "subscriptionArn" to AttributeValue(FAKE_SNS_SUBSCRIPTION_ARN),
                                "active" to AttributeValue("true")
                        )
                ) to OperationType.REMOVE
        ))

        @Test
        fun `Client deleted event routed properly`() {
            DatabaseStreamProcessor().run(clientEvent, emptyList())
            // TODO: Verify that the event was routed to the correct handler.
        }
    }
}
