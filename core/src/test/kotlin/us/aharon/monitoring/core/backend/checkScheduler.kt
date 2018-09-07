/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

import us.aharon.monitoring.core.checks.ClientCheck
import us.aharon.monitoring.core.api.check
import us.aharon.monitoring.core.api.checks


class CheckSchedulerTest {

    @Test
    fun `Client Check - Interval less than 1 hour`() {
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
        assertEquals(expected.interval, checks.first().checks.first().interval)
        // TODO: Call the checkScheduler function and assert that the message is sent to the SNS topic.
        //checkScheduler(time, checks)
    }

}
