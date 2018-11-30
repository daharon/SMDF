/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.extensions

import com.amazonaws.services.sqs.AmazonSQS
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.util.Env


private const val SERVERLESS_CHECK_QUEUE = "SERVERLESS_CHECK_QUEUE"


class ServerlessCheckQueueExtension : KoinTest, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val env: Env by inject()
    private val sqs: AmazonSQS by inject()

    override fun beforeTestExecution(context: ExtensionContext) {
        sqs.createQueue(SERVERLESS_CHECK_QUEUE)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        sqs.deleteQueue(env.get(SERVERLESS_CHECK_QUEUE))
    }
}
