/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.notificationprocessor

import cloud.localstack.LocalstackExtension
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.backend.NotificationProcessor
import us.aharon.monitoring.core.common.SQSTestEvent
import us.aharon.monitoring.core.common.TestLambdaContext
import us.aharon.monitoring.core.common.TestNotificationHandler
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.CheckResultStatus
import us.aharon.monitoring.core.extensions.DynamoDBTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension

import java.time.ZonedDateTime


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(DynamoDBTableExtension::class))
class MinimalNotification : KoinTest {

    private val json: ObjectMapper by inject()
    private val checks = listOf(checks("test") {
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
    private val singleTestEvent = SQSTestEvent(listOf(
            mapOf(
                    "handler" to TestNotificationHandler::class.java.canonicalName,
                    "checkResult" to CheckResultRecord(
                            scheduledAt = ZonedDateTime.parse("2018-08-23T11:37:44Z"),
                            executedAt = ZonedDateTime.parse("2018-08-23T11:38:44Z"),
                            completedAt = ZonedDateTime.parse("2018-08-23T11:39:44Z"),
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
