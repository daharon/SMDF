/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.messages


internal sealed class CheckMessage

internal data class ClientCheckMessage(
        val group: String,
        val name: String,
        val command: String,
        val timeout: Int,
        val subscribers: List<String>
) : CheckMessage()

/**
 * Message class used internally between the [CheckScheduler] and the serverless check executor.
 */
internal data class ServerlessCheckMessage(
        val group: String,
        val name: String,
        /**
         * Fully qualified class name of the serverless check.
         */
        val executor: String,
        val timeout: Int
) : CheckMessage()