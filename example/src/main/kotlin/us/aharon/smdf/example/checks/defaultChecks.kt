/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.example.checks

import us.aharon.smdf.core.api.clientCheckTemplate
import us.aharon.smdf.core.api.serverlessCheckTemplate

import us.aharon.smdf.example.handlers.DefaultHandler


/**
 * Set default values for client checks.
 */
val defaultClientCheck = clientCheckTemplate {
    interval = 5
    handlers = listOf(DefaultHandler::class)
    tags = listOf("linux")
    contacts = listOf("devops")
    timeout = 30
    ttl = 90
    occurrences = 1
    onlyIf = { true }
    notIf = { false }
}

/**
 * Set default values for serverless checks.
 */
val defaultServerlessCheck = serverlessCheckTemplate {
    interval = 15
    timeout = 300  // Five minutes.
    onlyIf = { true }
}

