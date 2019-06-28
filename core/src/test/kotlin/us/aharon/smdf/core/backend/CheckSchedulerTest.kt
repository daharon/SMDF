/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.test.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.api.check
import us.aharon.smdf.core.api.checks
import us.aharon.smdf.core.api.serverlessCheck
import us.aharon.smdf.core.backend.messages.ServerlessCheckMessage
import us.aharon.smdf.core.extensions.ClientCheckTopicExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension
import us.aharon.smdf.core.extensions.ServerlessCheckQueueExtension
import us.aharon.smdf.core.util.Env

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class CheckSchedulerTest : KoinTest {

    @Nested
    @ExtendWith(
        LocalstackExtension::class,
        LoadModulesExtension::class,
        ClientCheckTopicExtension::class,
        ServerlessCheckQueueExtension::class)
    inner class ClientChecks {

        @Test
        fun `Check is scheduled`() {
            // Create application check.
            val checkName = "Interval < 1 hour"
            val minute = 5
            val time = DateTime(2018, 1, 1, 0, minute, DateTimeZone.UTC)
            val checks = listOf(
                    checks("test") {
                        check(checkName) {
                            interval = 5  // Minutes
                        }
                    }
            )

            // Run the Lambda handler.
            CheckScheduler().run(time, checks)
        }
    }

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(ClientCheckTopicExtension::class),
        ExtendWith(ServerlessCheckQueueExtension::class))
    inner class ServerlessChecks {

        private val env: Env by inject()
        private val json: ObjectMapper by inject()

        @Test
        fun `Check is scheduled`() {
            val sqs: AmazonSQS by inject()
            // Create application check.
            val checkName = "Interval < 1 hour"
            val minute = 5
            val time = DateTime(2018, 1, 1, 0, minute, DateTimeZone.UTC)
            val checks = listOf(
                    checks("test") {
                        serverlessCheck(checkName) {
                            interval = 5  // Minutes
                        }
                    }
            )

            // Run the Lambda handler.
            CheckScheduler().run(time, checks)

            // Read message from serverless check queue.
            val request = ReceiveMessageRequest(env.get("SERVERLESS_CHECK_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val message = sqs.receiveMessage(request).messages.firstOrNull()
            assertNotNull(message)
            val parsedMsg = json.readValue<ServerlessCheckMessage>(message.body)
            assertEquals(checkName, parsedMsg.name)
            assertEquals("test", parsedMsg.group)
        }

        @Test
        fun `Check is NOT scheduled`() {
            val sqs: AmazonSQS by inject()
            // Create application check.
            val checkName = "test"
            val time = DateTime(2018, 1, 1, 0, 1, DateTimeZone.UTC)
            val checks = listOf(
                    checks("test") {
                        serverlessCheck(checkName) {
                            interval = 5  // Minutes
                        }
                    }
            )

            // Run the Lambda handler.
            CheckScheduler().run(time, checks)

            // Read message from serverless check queue.
            val request = ReceiveMessageRequest(env.get("SERVERLESS_CHECK_QUEUE"))
                    .withMaxNumberOfMessages(1)
            val message = sqs.receiveMessage(request).messages.firstOrNull()
            assertNull(message)
        }
    }
}
