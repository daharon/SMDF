/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.koin.standalone.inject

import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.util.Env

import java.util.UUID


/**
 * API Gateway handler that registers a client for monitoring.
 */
class ClientRegistrationHandler : BaseRequestHandler() {

    private val env: Env by inject()
    private val db: DynamoDBMapper by inject()
    private val sqs: AmazonSQS by inject()
    private val sns: AmazonSNS by inject()
    /**
     * New client queues are subscribed to this SNS Topic.
     */
    private val SNS_CLIENT_CHECK_TOPIC_ARN: String by lazy { env.get("CLIENT_CHECK_TOPIC") }

    companion object {
        private const val SNS_MESSAGE_ATTRIBUTE_TAGS = "tags"
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
        log.info("Client Name:  ${data.name}")
        log.info("Client Tags:  ${data.tags}")

        // Check to see if the client already exists in the database.
        val existingClient: ClientRecord? = getExistingClient(data.name!!)

        val response: APIGatewayProxyResponseEvent = if (existingClient == null) {
            // New client.  Create queue.
            val queueName = "monitoring-${UUID.randomUUID()}"
            log.info("Generated queue name:  $queueName")
            // TODO: Assign tags to queue.  App and client name.
            val queueRequest = CreateQueueRequest(queueName)
                    .withAttributes(mapOf("MessageRetentionPeriod" to "3600"))
            val queueResult = sqs.createQueue(queueRequest)
            // Get queue ARN.  For some reason this is not returned in the createQueue() result.
            val queueAttrRequest = GetQueueAttributesRequest(queueResult.queueUrl)
                    .withAttributeNames("QueueArn")
            val queueArn = sqs.getQueueAttributes(queueAttrRequest).attributes["QueueArn"]
            log.info("Created queue:  $queueArn")

            // Allow SNS topic to send messages to the queue.
            val queuePermRequest = SetQueueAttributesRequest()
                    .withQueueUrl(queueResult.queueUrl)
                    .withAttributes(mapOf(
                            "Policy" to generateQueuePermissionPolicy(queueArn!!, SNS_CLIENT_CHECK_TOPIC_ARN)
                    ))
            val queuePermResult = sqs.setQueueAttributes(queuePermRequest)
            log.info("Set permission policy for queue.")

            // Subscribe queue to SNS Topic.
            val subscribeRequest = SubscribeRequest()
                    .withTopicArn(SNS_CLIENT_CHECK_TOPIC_ARN)
                    .withProtocol("sqs")
                    .withEndpoint(queueArn)
                    .withAttributes(mapOf(
                            "FilterPolicy" to json.writeValueAsString(mapOf(
                                    SNS_MESSAGE_ATTRIBUTE_TAGS to data.tags
                            ))
                    ))
            val subscribeResult = sns.subscribe(subscribeRequest)
            log.info("Subscribed queue to SNS topic:  ${subscribeResult.subscriptionArn}")

            // Write client to database.
            val clientRecord = ClientRecord(
                    name = data.name,
                    tags = data.tags,
                    queueArn = queueArn,
                    queueUrl = queueResult.queueUrl,
                    subscriptionArn = subscribeResult.subscriptionArn)
            db.save(clientRecord)
            log.info("Saved client to database:  $clientRecord")

            // Response
            val responseBody = json.writeValueAsString(
                    ClientRegistrationResponse(queueArn))
            APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody)
        } else {
            // Existing client.  Verify that queue and subscription still exist.
            log.info("Client already registered:  $existingClient")
            val responseBody = json.writeValueAsString(
                    ClientRegistrationResponse(existingClient.queueArn))
            APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody)
        }

        return response
    }

    /**
     * Get the client record give the request data.
     *
     * @param clientName The unique name of the client.
     * @return The client details if already existing in the database, or null.
     */
    private fun getExistingClient(clientName: String): ClientRecord? {
        val query = DynamoDBQueryExpression<ClientRecord>()
                .withKeyConditionExpression("#name = :name")
                .withExpressionAttributeNames(mapOf("#name" to "name"))
                .withExpressionAttributeValues(mapOf(":name" to AttributeValue(clientName)))
                .withConsistentRead(true)
                .withLimit(1)
        val result = db.query(ClientRecord::class.java, query)
        return result.firstOrNull()
    }

    /**
     * Generate the JSON IAM Policy for allowing an SNS topic to send a message to an SQS Queue.
     */
    private fun generateQueuePermissionPolicy(sqsQueueArn: String, snsTopicArn: String): String {
        val policy = mapOf(
                "Version" to "2012-10-17",
                "Statement" to listOf(mapOf(
                        "Sid" to UUID.randomUUID().toString(),
                        "Effect" to "Allow",
                        "Principal" to "*",
                        "Action" to "sqs:SendMessage",
                        "Resource" to sqsQueueArn,
                        "Condition" to mapOf(
                                "ArnEquals" to mapOf(
                                        "aws:SourceArn" to snsTopicArn
                                )
                        )
                ))
        )
        return json.writeValueAsString(policy)
    }
}
