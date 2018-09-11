/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.module.kotlin.readValue


/**
 * API Gateway handler that registers a client for monitoring.
 */
class ClientRegistrationHandler : BaseRequestHandler() {
    /**
     * New client queues are subscribed to this SNS Topic.
     */
    private val SNS_CLIENT_CHECK_TOPIC_ARN: String by lazy { System.getenv("CLIENT_CHECK_TOPIC") }

    companion object {
        private const val SNS_MESSAGE_ATTRIBUTE_SUBSCRIBERS = "subscribers"
    }

    /**
     * Handler that registers a client for monitoring.
     *
     * - Verify that the client name is unique.
     * - Create SQS queue for the client subscriber with a filter based on the client's provided tags.
     *
     * @return Client SQS queue for receiving scheduled checks.  Success or failure is returned based on response code.
     */
    override fun handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        log.info("Request Event:  $request")
        val data: ClientRegistrationRequest = json.readValue(request.body)
        log.info("Client Name:  ${data.clientName}")
        log.info("Client Tags:  ${data.clientTags}")

        // Check to see if the client already exists in the database.

        //

        val response = json.writeValueAsString(
                ClientRegistrationResponse("arn:aws:sqs:region:account-id:queuename"))
        return APIGatewayProxyResponseEvent()
                .withBody(response)
                .withStatusCode(200)
    }
}
