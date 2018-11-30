/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.extensions

import com.amazonaws.services.sns.AmazonSNS
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.standalone.inject
import org.koin.test.KoinTest


private const val CLIENT_CHECK_TOPIC = "CLIENT_CHECK_TOPIC"


class ClientCheckTopicExtension : KoinTest, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val sns: AmazonSNS by inject()

    override fun beforeTestExecution(context: ExtensionContext) {
        sns.createTopic(CLIENT_CHECK_TOPIC)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        sns.deleteTopic("arn:aws:sns:us-east-1:123456789012:$CLIENT_CHECK_TOPIC")
    }
}
