/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend.messages


data class ClientRegistrationRequest(
        var name: String? = null,
        var tags: List<String>? = null
)
