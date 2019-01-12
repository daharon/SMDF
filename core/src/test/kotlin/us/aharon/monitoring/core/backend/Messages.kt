/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.backend.messages.ClientCheckMessage
import us.aharon.monitoring.core.backend.messages.ServerlessCheckMessage
import us.aharon.monitoring.core.extensions.LoadModulesExtension

import java.time.ZonedDateTime
import kotlin.test.assertEquals


private const val TIMESTAMP: String = "2018-08-23T11:39:44Z"


class Messages : KoinTest {

    @Nested
    @ExtendWith(LoadModulesExtension::class)
    inner class Client {
        private val json: ObjectMapper by inject()

        @Test
        fun `JSON serialization`() {
            val testMsg = ClientCheckMessage(
                    scheduledAt = ZonedDateTime.parse(TIMESTAMP),
                    group = "test",
                    name = "test-check",
                    timeout = 60,
                    command = "true",
                    subscribers = listOf())
            val expectedMsg = "{\"scheduledAt\":\"$TIMESTAMP\",\"group\":\"test\",\"name\":\"test-check\",\"command\":\"true\",\"timeout\":60,\"subscribers\":[]}"

            val jsonMessage = json.writeValueAsString(testMsg)
            assertEquals(expectedMsg, jsonMessage)
        }
    }

    @Nested
    @ExtendWith(LoadModulesExtension::class)
    inner class Serverless {
        private val json: ObjectMapper by inject()

        @Test
        fun `JSON serialization`() {
            val executor = "com.example.mon.executor.ExampleServerlessCheck"
            val testMsg = ServerlessCheckMessage(
                    scheduledAt = ZonedDateTime.parse(TIMESTAMP),
                    group = "test",
                    name = "test-check",
                    executor = executor,
                    timeout = 60)
            val expectedMsg = "{\"scheduledAt\":\"$TIMESTAMP\",\"group\":\"test\",\"name\":\"test-check\",\"executor\":\"$executor\",\"timeout\":60}"

            val jsonMessage = json.writeValueAsString(testMsg)
            assertEquals(expectedMsg, jsonMessage)
        }
    }
}
