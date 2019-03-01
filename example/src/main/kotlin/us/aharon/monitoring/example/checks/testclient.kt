/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.checks

import us.aharon.monitoring.core.api.checks

import us.aharon.monitoring.example.handlers.PermissionsNotificationHandler


/**
 * Check for testing the monitoring client.
 */
val TEST_CLIENT_CHECKS = checks("test") {

    mapOf(
            "Ok check"       to "echo 'OK - Situation is normal' && exit 0",
            "Warning check"  to "echo 'WARNING - Situation may be in a bad state soon' && exit 1",
            "Critical check" to "echo 'CRITICAL - Situation is critical' && exit 2",
            "Unknown check"  to "echo 'UNKNOWN - Situation is in an unknown state' && exit 3"
    ).forEach { (name, chk) ->

        defaultClientCheck(name) {
            command     = chk
            tags        = listOf("test-client")
            interval    = 2
            handlers    += PermissionsNotificationHandler::class
        }
    }
}
