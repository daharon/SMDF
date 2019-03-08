/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di


internal val TEST_ENVIRONMENT_VARIABLES = mapOf<String, String?>(
        "ENVIRONMENT" to "TEST",
        "CLIENT_CHECK_TOPIC" to "CLIENT_CHECK_TOPIC",
        "SERVERLESS_CHECK_QUEUE" to "http://localhost:4576/queue/SERVERLESS_CHECK_QUEUE",
        "CHECK_RESULTS_QUEUE" to "http://localhost:4576/queue/CHECK_RESULTS_QUEUE",
        "DB_TABLE" to "FAKE_DB_TABLE",
        "NOTIFICATION_QUEUE" to "http://localhost:4576/queue/NOTIFICATION_QUEUE"
)
