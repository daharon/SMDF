/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.clientcleanup

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.ClientCleanup
import us.aharon.monitoring.core.common.DynamodbTestEvent
import us.aharon.monitoring.core.common.FAKE_SNS_SUBSCRIPTION_ARN
import us.aharon.monitoring.core.common.FAKE_SQS_QUEUE_ARN
import us.aharon.monitoring.core.common.FAKE_SQS_QUEUE_URL


// TODO:  Test is broken.  SNS & SQS with LocalStack not working correctly?
@Disabled
@ExtendWith(LocalstackExtension::class)
class RemoveTest : BaseTest() {

    private val sqs: AmazonSQS by inject()
    private val sns: AmazonSNS by inject()
    private val testEvent = DynamodbTestEvent(mapOf(
            StreamRecord().withOldImage(
                    mapOf<String, AttributeValue>(
                            "name" to AttributeValue("fake-name"),
                            "tags" to AttributeValue().withL(AttributeValue("fake-tag")),
                            "subscriptionArn" to AttributeValue(FAKE_SNS_SUBSCRIPTION_ARN),
                            "queueArn" to AttributeValue(FAKE_SQS_QUEUE_ARN),
                            "queueUrl" to AttributeValue(FAKE_SQS_QUEUE_URL)
                    )
            ) to OperationType.REMOVE
    ))


    @BeforeEach
    fun createSNSTopic() {
        sns.createTopic("CLIENT_CHECK_TOPIC")
        sns.createTopic("SERVERLESS_CHECK_TOPIC")
    }

    @AfterEach
    fun deleteSNSTopic() {
        sns.deleteTopic("CLIENT_CHECK_TOPIC")
        sns.deleteTopic("SERVERLESS_CHECK_TOPIC")
    }

    @Test
    fun `DynamoDB event REMOVE`() {
        ClientCleanup().run(testEvent)
    }
}
