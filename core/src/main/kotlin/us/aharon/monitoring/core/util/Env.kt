/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.util

import javax.naming.NameNotFoundException


private val ENVIRONMENT_VARIABLES = mapOf<String, String?>(
        "ENVIRONMENT" to System.getenv("ENVIRONMENT"),
        "CLIENT_CHECK_TOPIC" to System.getenv("CLIENT_CHECK_TOPIC"),
        "CHECK_RESULTS_QUEUE" to System.getenv("CHECK_RESULTS_QUEUE"),
        "SERVERLESS_CHECK_QUEUE" to System.getenv("SERVERLESS_CHECK_QUEUE"),
        "CLIENT_DB_TABLE" to System.getenv("CLIENT_DB_TABLE"),
        "CHECK_RESULTS_DB_TABLE" to System.getenv("CHECK_RESULTS_DB_TABLE"),
        "NOTIFICATION_QUEUE" to System.getenv("NOTIFICATION_QUEUE")
)

/**
 * Interface for retrieving environment variables.
 * Used via the dependency injector so that it may be overridden in testing.
 */
internal class Env(private val envVars: Map<String, String?> = ENVIRONMENT_VARIABLES) {

    /**
     * Retrieve the value of the environment variable specified by [name].
     *
     * @param name The name of the environment variable to read.
     */
    fun get(name: String): String =
            envVars[name] ?: throw NameNotFoundException("$name environment variable is not set.")
}
