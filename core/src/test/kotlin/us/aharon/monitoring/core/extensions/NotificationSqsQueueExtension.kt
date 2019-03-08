/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.extensions

import com.amazonaws.services.sqs.AmazonSQS
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.util.Env


class NotificationSqsQueueExtension : KoinTest,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val env: Env by inject()
    private val sqs: AmazonSQS by inject()


    override fun beforeTestExecution(context: ExtensionContext) {
        sqs.createQueue("NOTIFICATION_QUEUE")
    }

    override fun afterTestExecution(context: ExtensionContext) {
        sqs.deleteQueue(env.get("NOTIFICATION_QUEUE"))
    }
}