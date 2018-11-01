/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.checkresultprocessor

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.dynamodbv2.model.StreamRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.CheckResultProcessor
import us.aharon.monitoring.core.common.DynamodbTestEvent
import us.aharon.monitoring.core.db.CheckResultStatus


@ExtendWith(LocalstackExtension::class)
class CheckResultProcessorOk : BaseTest() {

    private val singleTestEvent = DynamodbTestEvent(mapOf(
            StreamRecord().withNewImage(
                    mapOf<String, AttributeValue>(
                            "name" to AttributeValue("server-1.example.com"),
                            "timestamp" to AttributeValue("2018-08-23T11:39:44Z"),
                            "status" to AttributeValue(CheckResultStatus.OK.name),
                            "output" to AttributeValue("OK: This check is A-OK")
                    )
            ) to OperationType.INSERT
    ))


    @Test
    fun `Receive a single OK check result`() {
        // Run the Lambda handler.
        CheckResultProcessor().run(singleTestEvent)

        // TODO: Verify test results.
    }
}
