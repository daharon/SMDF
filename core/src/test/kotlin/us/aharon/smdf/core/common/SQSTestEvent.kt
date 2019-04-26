/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.common

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.util.Md5Utils
import com.fasterxml.jackson.databind.ObjectMapper
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import java.time.ZonedDateTime
import java.util.UUID


/**
 * Generate an SQS test even as consumed from AWS Lambda functions.
 */
class SQSTestEvent(messageBodies: List<Map<String, Any>>) : SQSEvent(), KoinComponent {

    private val json: ObjectMapper by inject()


    init {
        this.records = messageBodies.map { messageBody ->
            val messageBodyString = json.writeValueAsString(messageBody)

            SQSEvent.SQSMessage().apply {
                messageId = UUID.randomUUID().toString()
                receiptHandle = "MessageReceiptHandle"
                attributes = mapOf(
                        "ApproximateReceiveCount" to "1",
                        "SentTimestamp" to ZonedDateTime.now().toEpochSecond().toString(),
                        "SenderId" to "123456789012",
                        "ApproximateFirstReceiveTimestamp" to ZonedDateTime.now().toEpochSecond().toString()
                )
                messageAttributes = emptyMap()
                md5OfBody = Md5Utils.md5AsBase64(messageBodyString.toByteArray())
                eventSource = "aws:sqs"
                eventSourceArn = "arn:aws:sqs:us-east-1:123456789012:FakeQueue"
                awsRegion = Regions.US_EAST_1.name
                body = messageBodyString
            }
        }
    }
}
