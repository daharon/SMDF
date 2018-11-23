/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di


internal val TEST_ENVIRONMENT_VARIABLES = mapOf<String, String?>(
        "CLIENT_CHECK_TOPIC" to "CLIENT_CHECK_TOPIC",
        "SERVERLESS_CHECK_TOPIC" to "SERVERLESS_CHECK_TOPIC",
        "CLIENT_DB_TABLE" to "CLIENT_DB_TABLE",
        "CHECK_RESULTS_QUEUE" to "CHECK_RESULTS_QUEUE",
        "CHECK_RESULTS_DB_TABLE" to "CHECK_RESULTS_DB_TABLE",
        "NOTIFICATION_QUEUE" to "NOTIFICATION_QUEUE"
)
