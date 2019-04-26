/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.example.checks

import us.aharon.smdf.core.api.checks
import us.aharon.smdf.core.api.check
import us.aharon.smdf.core.api.serverlessCheck

import us.aharon.smdf.example.executors.CheckRunningInUSEast1
import us.aharon.smdf.example.executors.CheckWithPermissions
import us.aharon.smdf.example.handlers.DefaultHandler


/**
 * Example check group.
 */
val SYSTEM_CHECKS = checks("system") {

    /**
     * Client Check example.
     * https://docs.sensu.io/sensu-core/1.4/reference/checks/#check-configuration
     */
    check("CPU") {
        command = "/usr/lib64/nagios/plugins/check_cpu --warning 75 --critical 85"
        tags = listOf("linux")
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
        timeout = 30 // Seconds.
        ttl = 90 // Seconds.
        occurrences = 1
    }

    /**
     * Client Check example with user defined default template.
     */
    defaultClientCheck("RSyslog is running") {
        command = "/usr/lib64/nagios/plugins/check_procs --critical 1:1 --command rsyslog"
        interval = 2  // Override the template value.
    }

    /**
     * Serverless Check example.
     */
    serverlessCheck("Running in us-east-1 availability zone") {
        executor = CheckRunningInUSEast1::class
        interval = 3
    }

    /**
     * Serverless Check example with user defined default template.
     */
    defaultServerlessCheck("User defined serverless check template") {
        executor = CheckRunningInUSEast1::class
        interval = 3
    }

    /**
     * Serverless check with IAM permissions.
     */
    defaultServerlessCheck("Check with IAM permissions") {
        executor = CheckWithPermissions::class
        interval = 3
    }
}
