/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.backend.messages


data class ClientDeregistrationResponse(
        var code: Int? = null,
        var message: String? = null
)
