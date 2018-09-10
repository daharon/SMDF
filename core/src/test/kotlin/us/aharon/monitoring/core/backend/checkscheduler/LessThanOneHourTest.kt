/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkscheduler

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.sns.AmazonSNS
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.standalone.inject
import org.koin.test.KoinTest
import kotlin.test.assertEquals

import us.aharon.monitoring.core.checks.ClientCheck
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.api.checks
import us.aharon.monitoring.core.api.serverlessCheck
import us.aharon.monitoring.core.backend.CheckScheduler
import us.aharon.monitoring.core.checks.ServerlessCheck
import us.aharon.monitoring.core.di.modules


@ExtendWith(LocalstackExtension::class)
class LessThanOneHourTest : KoinTest {

    companion object {
        private val CLIENT_CHECK_TOPIC = System.getenv("CLIENT_CHECK_TOPIC")
        private val SERVERLESS_CHECK_TOPIC = System.getenv("SERVERLESS_CHECK_TOPIC")

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            startKoin(listOf(modules), logger = PrintLogger())
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            stopKoin()
        }
    }

    @Test
    fun `Client Check - Interval less than 1 hour`() {
        // Create SNS Topics.
        val snsClient: AmazonSNS by inject()
        snsClient.createTopic(CLIENT_CHECK_TOPIC)
        snsClient.createTopic(SERVERLESS_CHECK_TOPIC)

        // Create application check.
        val checkName = "Interval < 1 hour"
        val minute = 5
        val time = DateTime(2018, 1, 1, 0, minute, DateTimeZone.UTC)
        val checks = listOf(
                checks {
                    check(checkName) {
                        interval = 5  // Minutes
                    }
                }
        )
        val expected = ClientCheck(checkName).apply {
            interval = 5
        }

        // Run the Lambda handler.
        CheckScheduler().run(time, checks)

        // TODO: Read the message published to the SNS Topic and verify that it matches.
        assertEquals(expected.interval, checks.first().checks.first().interval)
    }

    @Test
    fun `Serverless Check - Interval less than 1 hour`() {
        // Create SNS Topics.
        val snsClient: AmazonSNS by inject()
        snsClient.createTopic(CLIENT_CHECK_TOPIC)
        snsClient.createTopic(SERVERLESS_CHECK_TOPIC)

        // Create application check.
        val checkName = "Interval < 1 hour"
        val minute = 5
        val time = DateTime(2018, 1, 1, 0, minute, DateTimeZone.UTC)
        val checks = listOf(
                checks {
                    serverlessCheck(checkName) {
                        interval = 5  // Minutes
                    }
                }
        )
        val expected = ServerlessCheck(checkName).apply {
            interval = 5
        }

        // Run the Lambda handler.
        CheckScheduler().run(time, checks)

        // TODO: Read the message published to the SNS Topic and verify that it matches.
        assertEquals(expected.interval, checks.first().checks.first().interval)
    }
}
