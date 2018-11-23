/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.notificationprocessor

import cloud.localstack.LocalstackExtension
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.standalone.inject

import us.aharon.monitoring.core.BaseTest
import us.aharon.monitoring.core.backend.NotificationProcessor
import us.aharon.monitoring.core.common.SQSTestEvent


@ExtendWith(LocalstackExtension::class)
class RunHandler : BaseTest() {

    private val json: ObjectMapper by inject()
    private val singleTestEvent = SQSTestEvent(listOf(
            mapOf(
                    "notificationHandler" to "canonical.class.name",
                    "metadata" to "{}"
            )
    ))


    @Test
    fun `Run simple notification handler`() {
        NotificationProcessor().run(singleTestEvent)

        // TODO:  Create a test notification handler and assert that it ran.
    }
}
