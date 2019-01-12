/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.serverlesscheckprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.sqs.AmazonSQS
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
import us.aharon.monitoring.core.extensions.CheckResultsSqsQueueExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(CheckResultsSqsQueueExtension::class))
class SimpleServerlessCheck : KoinTest {

    private val sqs: AmazonSQS by inject()


    @Test
    fun `Serverless check returns OK`() {
        // Test data.
        val checkEvent = SQSTestEvent(listOf(
                mapOf(
                        "scheduledAt" to "2018-08-23T11:39:44Z",
                        "group" to "test",
                        "name" to "test-check",
                        "executor" to OkExecutor::class.java.canonicalName,
                        "timeout" to 300
                )
        ))
        val checks = listOf(checks("test") {
            serverlessCheck("test-check") {
                notification = "This check was a test"
                executor = OkExecutor::class
            }
        })
        val context = TestLambdaContext("TestServerlessCheckProcessor")

        ServerlessCheckProcessor().run(checkEvent, checks, context)
        // TODO:  Check the result queue.
    }

    @Test
    fun `Serverless check throws an exception`() {
        // Test data.
        val checkEvent = SQSTestEvent(listOf(
                mapOf(
                        "scheduledAt" to "2018-08-23T11:39:44Z",
                        "group" to "test",
                        "name" to "test-check",
                        "executor" to ExceptionExecutor::class.java.canonicalName,
                        "timeout" to 300
                )
        ))
        val checks = listOf(checks("test") {
            serverlessCheck("test-check") {
                notification = "This check was a test"
                executor = ExceptionExecutor::class
            }
        })
        val context = TestLambdaContext("TestServerlessCheckProcessor")

        ServerlessCheckProcessor().run(checkEvent, checks, context)
        // TODO:  Check the result queue.
    }
}
