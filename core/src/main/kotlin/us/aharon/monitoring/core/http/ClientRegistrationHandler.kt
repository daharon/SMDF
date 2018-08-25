/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler


class ClientRegistrationHandler : RequestHandler<ClientRegistrationRequest, ClientRegistrationResponse> {

    /**
     * API Gateway handler that registers a client for monitoring.
     *
     * - Verify that the client name is unique.
     * - Create SQS queue for the client subscriber with a filter based on the client's provided tags.
     *
     * @return Client SQS queue for receiving scheduled checks.  Success or failure is returned based on response code.
     */
    override fun handleRequest(requestClient: ClientRegistrationRequest, context: Context): ClientRegistrationResponse {
        println("Client Name:  ${requestClient.clientName}")
        println("Client Tags:  ${requestClient.clientTags}")
        return ClientRegistrationResponse(
                "arn:aws:sqs:region:account-id:queuename")
        TODO("Implement client registration")
    }

}
