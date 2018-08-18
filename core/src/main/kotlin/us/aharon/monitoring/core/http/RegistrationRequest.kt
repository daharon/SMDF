/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http


data class RegistrationRequest(
        val clientName: String?,
        val clientTags: List<String>?
)
