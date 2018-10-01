/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.DeleteQueueRequest
import com.amazonaws.services.sqs.model.QueueDoesNotExistException
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.db.ClientRecord


/**
 * Cleanup/delete resources created by the [us.aharon.monitoring.core.http.ClientRegistrationHandler] when
 * a client is deleted/modified in the Clients DynamoDB table.
 *
 * - SNS subscription
 * - SQS Queue
 */
internal class ClientCleanup : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val db: DynamoDBMapper by inject()
    private val sqs: AmazonSQS by inject()
    private val sns: AmazonSNS by inject()


    fun run(event: DynamodbEvent) = event.records.forEach {
        when (OperationType.fromValue(it.eventName)) {
            OperationType.MODIFY -> modify(it)
            OperationType.REMOVE -> remove(it)
            OperationType.INSERT -> log.info("Do nothing on ${it.eventName} event.")
            else -> log.error("Unknown event name provided:  ${it.eventName}")
        }
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
    }

    /**
     * A client was deleted.
     */
    private fun remove(record: DynamodbEvent.DynamodbStreamRecord) {
        val oldClientRecord = db.marshallIntoObject(ClientRecord::class.java, record.dynamodb.oldImage)
        log.info("Deleting resources used by client ${oldClientRecord.name}.")
        deleteSubscription(oldClientRecord.subscriptionArn)
        deleteQueue(oldClientRecord.queueArn, oldClientRecord.queueUrl)
    }

    /**
     * A client was modified.
     */
    private fun modify(record: DynamodbEvent.DynamodbStreamRecord) {
        val oldClientRecord = db.marshallIntoObject(ClientRecord::class.java, record.dynamodb.oldImage)
        val newClientRecord = db.marshallIntoObject(ClientRecord::class.java, record.dynamodb.newImage)
        log.info("Checking if any resources for client ${oldClientRecord.name} require deletion.")

        // Delete SNS subscription?
        if (newClientRecord.subscriptionArn != oldClientRecord.subscriptionArn) {
            deleteSubscription(oldClientRecord.subscriptionArn)
        }
        // Delete queue?
        if (newClientRecord.queueArn != oldClientRecord.queueArn) {
            deleteQueue(oldClientRecord.queueArn, oldClientRecord.queueUrl)
        }
    }
}
