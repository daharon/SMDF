/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.extensions

import com.amazonaws.services.sqs.AmazonSQS
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.test.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.util.Env


class CheckResultsSqsQueueExtension : KoinTest,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val env: Env by inject()
    private val sqs: AmazonSQS by inject()


    override fun beforeTestExecution(context: ExtensionContext) {
        sqs.createQueue("CHECK_RESULTS_QUEUE")
    }

    override fun afterTestExecution(context: ExtensionContext) {
        sqs.deleteQueue(env.get("CHECK_RESULTS_QUEUE"))
    }
}
