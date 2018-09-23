/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.util

import javax.naming.NameNotFoundException


private val ENVIRONMENT_VARIABLES = mapOf<String, String?>(
        "CLIENT_CHECK_TOPIC" to System.getenv("CLIENT_CHECK_TOPIC"),
        "CLIENT_CHECK_TOPIC" to System.getenv("CLIENT_CHECK_TOPIC"),
        "SERVERLESS_CHECK_TOPIC" to System.getenv("SERVERLESS_CHECK_TOPIC"),
        "CLIENT_DB_TABLE" to System.getenv("CLIENT_DB_TABLE")
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