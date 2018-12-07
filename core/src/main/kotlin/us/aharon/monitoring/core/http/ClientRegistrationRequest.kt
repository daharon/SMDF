/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http


data class ClientRegistrationRequest(
        var name: String? = null,
        var tags: List<String>? = null
)
