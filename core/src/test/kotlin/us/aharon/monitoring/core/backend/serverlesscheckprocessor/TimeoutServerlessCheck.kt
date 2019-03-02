/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.serverlesscheckprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(CheckResultsSqsQueueExtension::class))
class TimeoutServerlessCheck : KoinTest {

    private val env: Env by inject()
    private val sqs: AmazonSQS by inject()
    private val json: ObjectMapper by inject()


    @Test
    fun `Serverless check times out`() {
        // Test data.
        val checkEvent = SQSTestEvent(listOf(
                mapOf(
                        "scheduledAt" to "2018-08-23T11:39:44Z",
                        "group" to "test",
                        "name" to "test-check",
                        "executor" to TimeoutExecutor::class.java.canonicalName,
                        "timeout" to 2 // Seconds
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

        // Check the result queue.
        val sqsRequest = ReceiveMessageRequest()
                .withQueueUrl(env.get("CHECK_RESULTS_QUEUE"))
                .withMaxNumberOfMessages(1)
                .withWaitTimeSeconds(1)
        val sqsResult = sqs.receiveMessage(sqsRequest)
        val checkResult = json.readValue<CheckResultRecord>(sqsResult.messages.first().body)

        assertEquals(CheckResultStatus.UNKNOWN, checkResult.status)
        assert(checkResult.output?.contains(Regex("^Serverless check timed out")) ?: false)
    }
}
