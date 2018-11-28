/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.notificationprocessor

import cloud.localstack.LocalstackExtension
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.NotificationProcessor
import us.aharon.monitoring.core.common.SQSTestEvent
import us.aharon.monitoring.core.common.TestLambdaContext
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus

import java.time.ZonedDateTime


@ExtendWith(LocalstackExtension::class)
class MinimalNotification : BaseTest() {

    private val json: ObjectMapper by inject()
    private val checks = listOf(checks("test") {
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
    private val singleTestEvent = SQSTestEvent(listOf(
            mapOf(
                    "handler" to TestNotificationHandler::class.java.canonicalName,
                    "checkResult" to CheckResultRecord(
                            timestamp = ZonedDateTime.parse("2018-08-23T11:39:44Z"),
                            group = "test",
                            name = "test-check",
                            source = "test-client-1.example.com",
                            status = CheckResultStatus.OK,
                            output = "OK: Check was successful"
                    )
            )
    ))


    @Test
    fun `Run simple notification handler`() {
        val context = TestLambdaContext("TestNotificationProcessor")
        NotificationProcessor().run(singleTestEvent, checks, context)
    }
}
