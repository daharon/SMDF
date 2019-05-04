/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.smdf.core.backend.messages.ClientDeregistrationRequest
import us.aharon.smdf.core.backend.messages.ClientDeregistrationResponse
import us.aharon.smdf.core.db.Dao


/**
 * Lambda handler that deactivates a client.
 *
 * This handler only updates the client's `active` attribute in the database.
 * Cleanup of the client's resources (SQS, SNS) is handled by [ClientCleanup]
 * via the [DatabaseStreamProcessor].
 */
internal class ClientDeregistration : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val db: Dao by inject()


    fun run(event: ClientDeregistrationRequest): ClientDeregistrationResponse {
        if (event.name.isNullOrEmpty()) {
            log.error("Empty or null value provided for client name.")
            return ClientDeregistrationResponse(400, "Cannot de-register NULL or empty client name.")
        }

        val client = db.getClient(event.name!!)
        if (client == null) {
            val message = "Client `${event.name}` not found."
            log.error(message)
            return ClientDeregistrationResponse(404, message)
        }
        // Update client data:  active = false
        db.saveClient(
                client.copy(active = false).apply { createdAt = client.createdAt },
                "Deactivated")
        log.info("De-activated client:  ${event.name}")
        return ClientDeregistrationResponse(200, "Deactivated client.")
    }
}
