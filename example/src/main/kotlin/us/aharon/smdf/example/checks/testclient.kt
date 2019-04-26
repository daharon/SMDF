/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.example.checks

import us.aharon.smdf.core.api.checks

import us.aharon.smdf.example.handlers.PermissionsNotificationHandler


/**
 * Check for testing the monitoring client.
 */
val TEST_CLIENT_CHECKS = checks("test") {

    val clientTags = listOf("test-client")

    mapOf(
            "Ok check"       to "echo 'OK - Situation is normal' && exit 0",
            "Warning check"  to "echo 'WARNING - Situation may be in a bad state soon' && exit 1",
            "Critical check" to "echo 'CRITICAL - Situation is critical' && exit 2",
            "Unknown check"  to "echo 'UNKNOWN - Situation is in an unknown state' && exit 3"
    ).forEach { (name, chk) ->

        defaultClientCheck(name) {
            command     = chk
            tags        = clientTags
            interval    = 2
            handlers    += PermissionsNotificationHandler::class
        }
    }

    defaultClientCheck("Timeout") {
        command  = "echo 'This check should time out.' && sleep 30"
        tags     = clientTags
        interval = 5
        timeout  = 5
    }
}
