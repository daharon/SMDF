/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http


data class ClientRegistrationRequest(
        var clientName: String? = null,
        var clientTags: List<String>? = null
)
