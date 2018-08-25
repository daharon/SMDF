/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http


data class ClientRegistrationRequest(
        val clientName: String? = null,
        val clientTags: List<String>? = null
)
