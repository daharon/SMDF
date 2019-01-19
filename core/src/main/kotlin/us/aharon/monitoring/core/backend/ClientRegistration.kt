/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.backend.messages.ClientRegistrationRequest
import us.aharon.monitoring.core.backend.messages.ClientRegistrationResponse
import us.aharon.monitoring.core.db.Dao
import us.aharon.monitoring.core.util.Env

import java.util.UUID


/**
 * API Gateway handler that registers a client for monitoring.
 */
class ClientRegistration : KoinComponent {

    private val env: Env by inject()
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val db: Dao by inject()
    private val sqs: AmazonSQS by inject()
    private val sns: AmazonSNS by inject()

    private val ENVIRONMENT: String by lazy { env.get("ENVIRONMENT") }
    /**
     * New client queues are subscribed to this SNS Topic.
     */
    private val SNS_CLIENT_CHECK_TOPIC_ARN: String by lazy { env.get("CLIENT_CHECK_TOPIC") }
    /**
     * Client must send its results to this queue.
     */
    private val SQS_CLIENT_RESULTS_QUEUE: String by lazy { env.get("CHECK_RESULTS_QUEUE") }

    companion object {
        private const val MESSAGE_RETENTION_PERIOD = 3600  // One hour
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
     */
    fun run(event: ClientRegistrationRequest): ClientRegistrationResponse {
        log.info("Client Name:  ${event.name}")
        log.info("Client Tags:  ${event.tags}")
        if (event.name.isNullOrEmpty() || event.tags == null) {
            throw Exception("Invalid values in registration request:  $event")
        }

        // Check to see if the client already exists in the database.
        val existingClient: ClientRecord? = db.getClient(event.name!!)

        return when (existingClient) {
            is ClientRecord -> {
                // Existing client.
                log.info("Client already registered:  $existingClient")
                if (!queueExists(existingClient.queueUrl) || !subscriptionExists(existingClient.subscriptionArn)) {
                    log.error("Must create new queue and/or subscription!")
                    val qns = createQueueAndSubscription(existingClient.name, existingClient.tags)
                    // Update client in database.
                    val clientRecord = existingClient.copy(
                            queueArn = qns.queueArn,
                            queueUrl = qns.queueUrl,
                            subscriptionArn = qns.subscriptionArn)
                    db.saveClient(clientRecord, "Updated client with tags ${clientRecord.tags}.")
                    log.info("Saved client to database:  $clientRecord")
                    ClientRegistrationResponse(
                            commandQueue = qns.queueUrl,
                            resultQueue = SQS_CLIENT_RESULTS_QUEUE)
                } else {
                    ClientRegistrationResponse(
                            commandQueue = existingClient.queueUrl!!,
                            resultQueue = SQS_CLIENT_RESULTS_QUEUE)
                }
            }
            else -> {
                // New client.
                log.info("New client.")
                val qns = createQueueAndSubscription(event.name, event.tags)
                // Write client to database.
                val clientRecord = ClientRecord(
                        name = event.name,
                        tags = event.tags,
                        queueArn = qns.queueArn,
                        queueUrl = qns.queueUrl,
                        subscriptionArn = qns.subscriptionArn)
                db.saveClient(clientRecord, "Created client with tags ${clientRecord.tags}.")
                log.info("Saved client to database:  $clientRecord")

                ClientRegistrationResponse(
                        commandQueue = qns.queueUrl,
                        resultQueue = SQS_CLIENT_RESULTS_QUEUE)
            }
        }
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
        val queueName = "monitoring-$ENVIRONMENT-${UUID.randomUUID()}"
        log.info("Generated queue name:  $queueName")
        val queueRequest = CreateQueueRequest(queueName)
                .withAttributes(mapOf("MessageRetentionPeriod" to MESSAGE_RETENTION_PERIOD.toString()))
        val queueResult = sqs.createQueue(queueRequest)
        // Assign tags to the queue.
        sqs.tagQueue(queueResult.queueUrl, mapOf(
                "App" to "Monitoring",
                "Env" to ENVIRONMENT,
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
                        )),
                        "RawMessageDelivery" to true.toString()
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
