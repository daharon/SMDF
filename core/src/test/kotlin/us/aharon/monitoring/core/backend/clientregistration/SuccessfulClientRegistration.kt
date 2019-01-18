/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.clientregistration

import cloud.localstack.LocalstackExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.test.KoinTest

import us.aharon.monitoring.core.backend.ClientRegistration
import us.aharon.monitoring.core.backend.messages.ClientRegistrationRequest
import us.aharon.monitoring.core.extensions.ClientCheckTopicExtension
import us.aharon.monitoring.core.extensions.ClientsTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(ClientCheckTopicExtension::class),
    ExtendWith(ClientsTableExtension::class))
class SuccessfulClientRegistration : KoinTest {

    companion object {
        private const val CLIENT_CHECK_TOPIC = "CLIENT_CHECK_TOPIC"
    }


    @Test
    fun `Register client`() {
        val registrationRequest = ClientRegistrationRequest(
                name = "test-client",
                tags = listOf("tag-1", "tag-2"))

        val registrationResponse = ClientRegistration().run(registrationRequest)
        assert(registrationResponse.commandQueue.isNotEmpty())
        assert(registrationResponse.resultQueue.isNotEmpty())
        // TODO: Query database for client record.
    }
}
