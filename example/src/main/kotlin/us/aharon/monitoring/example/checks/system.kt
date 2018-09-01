/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.checks

import us.aharon.monitoring.core.checks.checks
import us.aharon.monitoring.core.checks.check
import us.aharon.monitoring.core.checks.serverlessCheck

import us.aharon.monitoring.example.handlers.DefaultHandler


val SYSTEM_CHECKS = checks {

    /**
     * https://docs.sensu.io/sensu-core/1.4/reference/checks/#check-configuration
     */
    check("CPU") {
        command = "/usr/lib64/nagios/plugins/check_cpu --warning 75 --critical 85"
        subscribers = listOf("linux")
        interval = 1 // Minutes
        handlers = listOf(DefaultHandler::class)
        highFlapThreshold = 10
        lowFlapThreshold = 2
        notification = "CPU usage is high"
        additional = mapOf(
                "extra metadata" to "be recorded along with the check's other data."
        )
        contacts = listOf("devops")
        volatile = false
        timeout = 30 // Seconds.  Not sure if this is necessary.
        ttl = 90 // Seconds.
        autoResolve = true  // Non-MVP feature.
        occurrences = 1
        subdue = ""  // A set time period to dis-able this check.
    }

    serverlessCheck("404 Responses") {
        command = "us.aharon.monitoring.example.GraphiteCheck404s"
        interval = 3
    }
}
