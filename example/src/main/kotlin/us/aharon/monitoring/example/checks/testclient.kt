/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.checks

import us.aharon.monitoring.core.api.checks


/**
 * Check for testing the monitoring client.
 */
val TEST_CLIENT_CHECKS = checks("test") {

    mapOf(
            "Ok check"       to "echo 'Checking OK' && exit 0",
            "Warning check"  to "echo 'Checking WARNING' && exit 1",
            "Critical check" to "echo 'Checking CRITICAL' && exit 2",
            "Unknown check"  to "echo 'Checking UNKNOWN' && exit 3"
    ).forEach { (name, chk) ->

        defaultClientCheck(name) {
            command     = chk
            subscribers = listOf("test-client")
            interval    = 2
        }
    }
}
