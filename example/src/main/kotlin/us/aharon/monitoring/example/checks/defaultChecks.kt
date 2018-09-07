/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.checks

import us.aharon.monitoring.core.api.clientCheckTemplate
import us.aharon.monitoring.core.api.serverlessCheckTemplate

import us.aharon.monitoring.example.handlers.DefaultHandler


/**
 * Set default values for client checks.
 */
val defaultClientCheck = clientCheckTemplate {
    interval = 5
    handlers = listOf(DefaultHandler::class)
    subscribers = listOf("linux")
    contacts = listOf("devops")
    timeout = 30
    ttl = 90
    occurrences = 1
}

/**
 * Set default values for serverless checks.
 */
val defaultServerlessCheck = serverlessCheckTemplate {
    interval = 15
    timeout = 300  // Five minutes.
}

