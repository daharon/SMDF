/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend.messages

import java.time.ZonedDateTime


internal sealed class CheckMessage

/**
 * Check message sent to the remote client process.
 */
internal data class ClientCheckMessage(
        val scheduledAt: ZonedDateTime,
        val group: String,
        val name: String,
        val command: String,
        val timeout: Int,
        val tags: List<String>
) : CheckMessage()

/**
 * Message class used internally between the [CheckScheduler] and the serverless check executor.
 */
internal data class ServerlessCheckMessage(
        val scheduledAt: ZonedDateTime,
        val group: String,
        val name: String,
        /**
         * Fully qualified class name of the serverless check.
         */
        val executor: String,
        val timeout: Int
) : CheckMessage()
