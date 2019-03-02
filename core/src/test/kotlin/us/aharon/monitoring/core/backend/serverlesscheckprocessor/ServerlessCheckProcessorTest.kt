/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.serverlesscheckprocessor

import cloud.localstack.LocalstackExtension
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
import us.aharon.monitoring.core.api.serverlessCheck
import us.aharon.monitoring.core.backend.ServerlessCheckProcessor
import us.aharon.monitoring.core.common.SQSTestEvent
import us.aharon.monitoring.core.common.TestLambdaContext
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.extensions.CheckResultsSqsQueueExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension
import us.aharon.monitoring.core.util.Env

import kotlin.test.assertEquals


class ServerlessCheckProcessorTest : KoinTest {

    private val env: Env by inject()
    private val sqs: AmazonSQS by inject()
    private val json: ObjectMapper by inject()


    /**
     * Read a single message from the check result queue.
     */
    private fun resultQueueGetOne(): CheckResultRecord {
        val sqsRequest = ReceiveMessageRequest()
                .withQueueUrl(env.get("CHECK_RESULTS_QUEUE"))
                .withMaxNumberOfMessages(1)
                .withWaitTimeSeconds(1)
        val sqsResult = sqs.receiveMessage(sqsRequest)
        return json.readValue<CheckResultRecord>(sqsResult.messages.first().body)
    }


    /**
     * Test basic/minimal serverless check processor functionality.
     */
    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(CheckResultsSqsQueueExtension::class))
    inner class Minimal {

        @Test
        fun `Serverless check returns OK`() {
            // Test data.
            val checkEvent = SQSTestEvent(listOf(
                    mapOf(
                            "scheduledAt" to "2018-08-23T11:39:44Z",
                            "group"       to "test",
                            "name"        to "test-check",
                            "executor"    to OkExecutor::class.java.canonicalName,
                            "timeout"     to 300
                    )
            ))
            val checks = listOf(checks("test") {
                serverlessCheck("test-check") {
                    notification = "This check was a test"
                    executor     = OkExecutor::class
                }
            })
            val context = TestLambdaContext("TestServerlessCheckProcessor")

            ServerlessCheckProcessor().run(checkEvent, checks, context)

            val result = resultQueueGetOne()
            assertEquals(CheckResultStatus.OK, result.status)
        }

        @Test
        fun `Serverless check throws an exception`() {
            // Test data.
            val checkEvent = SQSTestEvent(listOf(
                    mapOf(
                            "scheduledAt" to "2018-08-23T11:39:44Z",
                            "group"       to "test",
                            "name"        to "test-check",
                            "executor"    to ExceptionExecutor::class.java.canonicalName,
                            "timeout"     to 300
                    )
            ))
            val checks = listOf(checks("test") {
                serverlessCheck("test-check") {
                    notification = "This check should throw an exception"
                    executor     = ExceptionExecutor::class
                }
            })
            val context = TestLambdaContext("TestServerlessCheckProcessor")

            ServerlessCheckProcessor().run(checkEvent, checks, context)

            val result = resultQueueGetOne()
            assertEquals(CheckResultStatus.CRITICAL, result.status)
            assert(result.output?.contains(Regex("^Error running serverless check")) ?: false)
        }
    }


    /**
     * Test proper handling of serverless check time-outs.
     */
    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(CheckResultsSqsQueueExtension::class))
    inner class Timeout {
        @Test
        fun `Serverless check times out`() {
            // Test data.
            val checkEvent = SQSTestEvent(listOf(
                    mapOf(
                            "scheduledAt" to "2018-08-23T11:39:44Z",
                            "group"       to "test",
                            "name"        to "test-check",
                            "executor"    to TimeoutExecutor::class.java.canonicalName,
                            "timeout"     to 2 // Seconds
                    )
            ))
            val checks = listOf(checks("test") {
                serverlessCheck("test-check") {
                    notification = "This check should time out"
                    executor     = TimeoutExecutor::class
                    timeout      = 2 // Seconds
                }
            })
            val context = TestLambdaContext("TestServerlessCheckProcessor")

            ServerlessCheckProcessor().run(checkEvent, checks, context)

            val checkResult = resultQueueGetOne()
            assertEquals(CheckResultStatus.UNKNOWN, checkResult.status)
            assert(checkResult.output?.contains(Regex("^Serverless check timed out")) ?: false)
        }
    }
}
