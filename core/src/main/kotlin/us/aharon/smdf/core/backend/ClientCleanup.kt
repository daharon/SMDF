/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.InvalidParameterException
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.DeleteQueueRequest
import com.amazonaws.services.sqs.model.QueueDoesNotExistException
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.core.KoinComponent
import org.koin.core.inject

import us.aharon.smdf.core.db.Dao


/**
 * Cleanup/delete resources created by the [us.aharon.smdf.core.backend.ClientRegistration] when
 * a client is deleted/modified in the Clients DynamoDB table.
 *
 * - SNS subscription
 * - SQS Queue
 */
internal class ClientCleanup : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val db: Dao by inject()
    private val sqs: AmazonSQS by inject()
    private val sns: AmazonSNS by inject()


    fun run(record: DynamodbEvent.DynamodbStreamRecord) =
            when (OperationType.fromValue(record.eventName)) {
                OperationType.MODIFY -> modify(record)
                OperationType.REMOVE -> remove(record)
                OperationType.INSERT -> log.info("Do nothing on ${record.eventName} event.")
                else -> log.error("Unknown event name provided:  ${record.eventName}")
            }

    /**
     * Delete SQS Queue.
     */
    private fun deleteQueue(queueArn: String?, queueUrl: String?) = try {
        val deleteQueueRequest = DeleteQueueRequest(queueUrl)
        sqs.deleteQueue(deleteQueueRequest)
        log.info("Deleted SQS queue:  $queueArn")
    } catch (e: QueueDoesNotExistException) {
        log.warn("Queue does not exist:  $queueArn")
    }

    /**
     * Delete SNS subscription.
     */
    private fun deleteSubscription(subscriptionArn: String?) = try {
        val unsubscribeRequest = UnsubscribeRequest(subscriptionArn)
        sns.unsubscribe(unsubscribeRequest)
        log.info("Deleted SNS subscription:  $subscriptionArn")
    } catch (e: NotFoundException) {
        log.warn("SNS subscription does not exist:  $subscriptionArn")
    } catch (e: InvalidParameterException) {
        log.debug("SNS subscription ARN was invalid:  $subscriptionArn")
    }

    /**
     * A client was deleted.
     */
    private fun remove(record: DynamodbEvent.DynamodbStreamRecord) {
        val oldClientRecord = db.marshallClientRecord(record.dynamodb.oldImage)
        log.info("Deleting resources used by client ${oldClientRecord.name}.")
        deleteSubscription(oldClientRecord.subscriptionArn)
        deleteQueue(oldClientRecord.queueArn, oldClientRecord.queueUrl)
    }

    /**
     * A client was modified.
     * If the client was de-activated or its record was deleted from the database,
     * then delete its queue and SNS subscription.
     */
    private fun modify(record: DynamodbEvent.DynamodbStreamRecord) {
        val oldClientRecord = db.marshallClientRecord(record.dynamodb.oldImage)
        val newClientRecord = db.marshallClientRecord(record.dynamodb.newImage)

        log.info("Checking if any resources for client ${oldClientRecord.name} require deletion.")
        // Delete SNS subscription?
        if (newClientRecord.subscriptionArn != oldClientRecord.subscriptionArn) {
            deleteSubscription(oldClientRecord.subscriptionArn)
        }
        // Delete queue?
        if (newClientRecord.queueArn != oldClientRecord.queueArn) {
            deleteQueue(oldClientRecord.queueArn, oldClientRecord.queueUrl)
        }

        // Was this client de-activated?
        if (!newClientRecord.active!! && oldClientRecord.active!!) {
            log.info("Client ${newClientRecord.name} has been de-activated.")
            deleteSubscription(newClientRecord.subscriptionArn)
            deleteQueue(newClientRecord.queueArn, newClientRecord.queueUrl)
            // Clear the appropriate fields for the client record.
            db.saveClient(
                    newClientRecord.copy(queueArn = null, queueUrl = null, subscriptionArn = null).apply {
                        createdAt = newClientRecord.createdAt
                    },
                    """Client queue and subscription deleted.
                        |Queue: ${newClientRecord.queueArn}
                        |Subscription: ${newClientRecord.subscriptionArn}""".trimMargin())
        }
    }
}
