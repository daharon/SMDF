/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di


internal val TEST_ENVIRONMENT_VARIABLES = mapOf<String, String?>(
        "CLIENT_CHECK_TOPIC" to "CLIENT_CHECK_TOPIC",
        "SERVERLESS_CHECK_QUEUE" to "http://localhost:4576/queue/SERVERLESS_CHECK_QUEUE",
        "CLIENT_DB_TABLE" to "CLIENT_DB_TABLE",
        "CHECK_RESULTS_QUEUE" to "http://localhost:4576/queue/CHECK_RESULTS_QUEUE",
        "CHECK_RESULTS_DB_TABLE" to "CHECK_RESULTS_DB_TABLE",
        "NOTIFICATION_QUEUE" to "NOTIFICATION_QUEUE"
)
