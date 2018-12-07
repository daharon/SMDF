/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.messages


data class ClientRegistrationResponse(
        val commandQueue: String,
        val resultQueue: String
)
