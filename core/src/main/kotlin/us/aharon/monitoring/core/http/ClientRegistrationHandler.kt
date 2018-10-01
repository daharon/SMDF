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
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.*
import com.fasterxml.jackson.databind.JsonMappingException
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
     * Queue and subscription creation results.
     */
    private data class NewQueueAndSubscription(
            val queueArn: String,
            val queueUrl: String,
            val subscriptionArn: String
    )

    /**
     * Handler that registers a client for monitoring.
     *
     * - Verify that the client name is unique.
     * - Create SQS queue for the client subscriber with a filter based on the client's provided tags.
     * - Subscribe the SQS queue to the CLIENT_CHECK_TOPIC.
     * - If the client already exists but either the queue or the subscription is missing, then
     *   re-create the queue and subscription.  The [us.aharon.monitoring.core.backend.ClientCleanup] handler will
     *   deal with any cleanup that is necessary.
     *
     * @return Client SQS queue for receiving scheduled checks.  Success or failure is returned based on response code.
     */
    override fun handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        log.info("Request Event:  $request")
        val data: ClientRegistrationRequest = try {
            json.readValue(request.body)
        } catch (e: JsonMappingException) {
            return APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"errorMessage\": \"${e.message}\"}")
        }
        log.info("Client Name:  ${data.name}")
        log.info("Client Tags:  ${data.tags}")

        // Check to see if the client already exists in the database.
        val existingClient: ClientRecord? = getExistingClient(data.name)

        val response: APIGatewayProxyResponseEvent = when (existingClient) {
            is ClientRecord -> {
                // Existing client.
                log.info("Client already registered:  $existingClient")
                val responseBody: String = if (!queueExists(existingClient.queueUrl) ||
                        !subscriptionExists(existingClient.subscriptionArn)) {
                    log.error("Must create new queue and/or subscription!")
                    val qns = createQueueAndSubscription(existingClient.name, existingClient.tags)
                    // Update and save client to database.
                    val clientRecord = ClientRecord(
                            name = existingClient.name,
                            tags = existingClient.tags,
                            queueArn = qns.queueArn,
                            queueUrl = qns.queueUrl,
                            subscriptionArn = qns.subscriptionArn)
                    db.save(clientRecord)
                    log.info("Saved client to database:  $clientRecord")
                    json.writeValueAsString(ClientRegistrationResponse(qns.queueArn))
                } else {
                    json.writeValueAsString(ClientRegistrationResponse(existingClient.queueArn))
                }

                APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(responseBody)
            }
            else -> {
                // New client.
                log.info("New client.")
                val qns = createQueueAndSubscription(data.name, data.tags)
                // Write client to database.
                val clientRecord = ClientRecord(
                        name = data.name,
                        tags = data.tags,
                        queueArn = qns.queueArn,
                        queueUrl = qns.queueUrl,
                        subscriptionArn = qns.subscriptionArn)
                db.save(clientRecord)
                log.info("Saved client to database:  $clientRecord")

                // Response
                val responseBody = json.writeValueAsString(
                        ClientRegistrationResponse(qns.queueArn))
                APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(responseBody)
            }
        }

        return response
    }

    /**
     * Create new SQS queue and SNS subscription based on the provided client name and tags.
     *
     * @param name The client name.
     * @param tags The client's subscription tags.
     * @return The SQS queue and SNS subscription that were created.
     */
    private fun createQueueAndSubscription(name: String?, tags: List<String>?): NewQueueAndSubscription {
        // Create queue.
        val queueName = "monitoring-${UUID.randomUUID()}"
        log.info("Generated queue name:  $queueName")
        val queueRequest = CreateQueueRequest(queueName)
                .withAttributes(mapOf("MessageRetentionPeriod" to "3600"))
        val queueResult = sqs.createQueue(queueRequest)
        // Assign tags to the queue.
        sqs.tagQueue(queueResult.queueUrl, mapOf(
                "App" to "Monitoring",
                "Client" to name))
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
        sqs.setQueueAttributes(queuePermRequest)
        log.info("Set permission policy for queue.")

        // Subscribe queue to SNS Topic.
        val subscribeRequest = SubscribeRequest()
                .withTopicArn(SNS_CLIENT_CHECK_TOPIC_ARN)
                .withProtocol("sqs")
                .withEndpoint(queueArn)
                .withAttributes(mapOf(
                        "FilterPolicy" to json.writeValueAsString(mapOf(
                                SNS_MESSAGE_ATTRIBUTE_TAGS to tags
                        ))
                ))
        val subscribeResult = sns.subscribe(subscribeRequest)
        log.info("Subscribed queue to SNS topic:  ${subscribeResult.subscriptionArn}")

        return NewQueueAndSubscription(
                queueArn = queueArn,
                queueUrl = queueResult.queueUrl,
                subscriptionArn = subscribeResult.subscriptionArn
        )
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

    /**
     * Query the queue attributes as a test to determine if the queue actually exists.
     */
    private fun queueExists(queueUrl: String?): Boolean = try {
        sqs.getQueueAttributes(queueUrl, emptyList())
        log.info("Queue exists:  $queueUrl")
        true
    } catch (e: QueueDoesNotExistException) {
        log.info("Queue does not exist:  $queueUrl")
        false
    }

    /**
     * Query the SNS subscription attributes as a test to determine if the subscription actually exists.
     */
    private fun subscriptionExists(subscriptionArn: String?): Boolean = try {
        sns.getSubscriptionAttributes(subscriptionArn)
        log.info("Subscription exists:  $subscriptionArn")
        true
    } catch (e: NotFoundException) {
        log.info("Subscription does not exist:  $subscriptionArn")
        false
    }
}
