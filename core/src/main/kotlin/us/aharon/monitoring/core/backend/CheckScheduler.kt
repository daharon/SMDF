/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.joda.time.DateTime
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.checks.ClientCheck
import us.aharon.monitoring.core.checks.ServerlessCheck
import us.aharon.monitoring.core.util.joinToSNSMessageAttributeStringValue

import java.util.concurrent.TimeUnit


private data class ClientCheckMessage(
        val name: String,
        val command: String,
        val timeout: Int,
        val subscribers: List<String>
)

private data class ServerlessCheckMessage(
        val name: String,
        val executor: String,
        val timeout: Int
)

internal class CheckScheduler : KoinComponent {

    private val SNS_CLIENT_CHECK_TOPIC_ARN: String by lazy { System.getenv("CLIENT_CHECK_TOPIC") }
    private val SNS_SERVERLESS_CHECK_TOPIC_ARN: String by lazy { System.getenv("SERVERLESS_CHECK_TOPIC") }
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }

    companion object {
        private const val SNS_MESSAGE_ATTRIBUTE_TAGS = "tags"
    }

    /**
     * Iterate through the [us.aharon.monitoring.core.Application]'s configured checks/check-groups
     * and push those that should be run now to the fanout queue to be executed.
     *
     * @param time The time provided by the [ScheduledEvent].
     * @param checks The [us.aharon.monitoring.core.Application]'s configured checks.
     */
    fun run(time: DateTime, checks: List<CheckGroup>) {
        // Get the scheduled event time from the event object in minutes since the epoch.
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time.millis)
        log.info { "$minutes minutes since the epoch." }

        // Iterate through the checks and determine which should be scheduled now.
        val scheduleChecks = mutableListOf<Check>()
        checks.forEach { checkGroup ->
            scheduleChecks += checkGroup.checks.filter {
                scheduleCheck(minutes, it)
            }
        }
        log.info { "Scheduling the following checks:  ${scheduleChecks.map { it.name }}" }

        // Construct the check objects to send to the SNS topic.
        val clientChecks: List<ClientCheckMessage> = scheduleChecks
                .filterIsInstance(ClientCheck::class.java)
                .map {
                    ClientCheckMessage(it.name, it.command, it.timeout, it.subscribers)
                }
        val serverlessChecks: List<ServerlessCheckMessage> = scheduleChecks
                .filterIsInstance(ServerlessCheck::class.java)
                .map {
                    ServerlessCheckMessage(it.name, it.executor.java.name, it.timeout)
                }

        // Send the client check messages to the SNS topic.
        sendClientChecks(clientChecks)

        // Send the serverless check messages to the SNS topic.
        sendServerlessChecks(serverlessChecks)
    }

    /**
     * Determine if the given check should be run now.
     *
     * @param minutes Minutes since the epoch.
     * @param check The [Check] to evaluate.
     * @return True if the [Check] should be scheduled now, false otherwise.
     */
    private fun scheduleCheck(minutes: Long, check: Check): Boolean {
        // TODO:  Account for checks that should not run every day, or only run during certain periods of the day. See [Check.subdue].
        return minutes % check.interval == 0L
    }

    /**
     * Publish the scheduled client checks to the SNS fanout topic.
     *
     * @param clientChecks The [ClientCheckMessage]'s that have been scheduled.
     */
    private fun sendClientChecks(clientChecks: List<ClientCheckMessage>) {
        val mapper = ObjectMapper()
        val snsClient: AmazonSNS by inject()

        clientChecks.forEach { check ->
            val jsonMessage = mapper.writeValueAsString(check)
            val publishReq = PublishRequest()
                    .withTopicArn(SNS_CLIENT_CHECK_TOPIC_ARN)
                    .withMessage(jsonMessage)
                    // Set message attributes so that the subscribed SQS queues can set filter policies.
                    .withMessageAttributes(
                            mapOf(
                                    SNS_MESSAGE_ATTRIBUTE_TAGS to MessageAttributeValue()
                                            .withDataType("String.Array")
                                            .withStringValue(check.subscribers.joinToSNSMessageAttributeStringValue())
                            )
                    )
            log.info { "SNS publish request:\n$publishReq" }
            val result = snsClient.publish(publishReq)
            log.info { "Published message ID:  ${result.messageId}" }
        }
    }

    /**
     * Publish the scheduled serverless checks to the SNS fanout topic.
     *
     * @param serverlessChecks The [ServerlessCheckMessage]'s that have been scheduled.
     */
    private fun sendServerlessChecks(serverlessChecks: List<ServerlessCheckMessage>) {
        val mapper = ObjectMapper()
        val snsClient: AmazonSNS by inject()

        serverlessChecks.forEach { check ->
            val jsonMessage = mapper.writeValueAsString(check)
            val publishReq = PublishRequest()
                    .withTopicArn(SNS_SERVERLESS_CHECK_TOPIC_ARN)
                    .withMessage(jsonMessage)
            log.info { "SNS publish request:\n$publishReq" }
            val result = snsClient.publish(publishReq)
            log.info { "Published message ID:  ${result.messageId}" }
        }
    }
}
