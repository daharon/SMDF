/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.test.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.backend.messages.ClientRegistrationRequest
import us.aharon.smdf.core.db.Dao
import us.aharon.smdf.core.extensions.ClientCheckTopicExtension
import us.aharon.smdf.core.extensions.DynamoDBTableExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension

import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class ClientRegistrationTest {

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(ClientCheckTopicExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class Success : KoinTest {
        @Test
        fun `Register client`() {
            val clientName = "test-client"
            val registrationRequest = ClientRegistrationRequest(
                    name = clientName,
                    tags = listOf("tag-1", "tag-2"))

            val registrationResponse = ClientRegistration().run(registrationRequest)
            assert(registrationResponse.commandQueue.isNotEmpty())
            assert(registrationResponse.resultQueue.isNotEmpty())

            // Verify that the client details were written to the database.
            val db: Dao by inject()
            val client = db.getClient(clientName)
            assertNotNull(client)
            assertEquals(registrationResponse.commandQueue, client.queueUrl)

            // Verify that the client command queue is tagged correctly.
            // FIXME: The Localstack SQS implementation does not support tagging, yet.
            /*
            val sqs: AmazonSQS by inject()
            val env: Env by inject()
            val queueTags = sqs.listQueueTags(client.queueUrl)
            assertEquals("SMDF", queueTags.tags["App"])
            assertEquals(env.get("ENVIRONMENT"), queueTags.tags["Env"])
            assertEquals(clientName, queueTags.tags["Client"])
            */
        }

        @Test
        fun `ECS task ID as client name`() {
            val clientName = "1efa1485-7f78-435e-8ff2-d21b43772ce1"
            val registrationRequest = ClientRegistrationRequest(
                    name = clientName,
                    tags = listOf("tag-1", "tag-2"))

            val registrationResponse = ClientRegistration().run(registrationRequest)
            assert(registrationResponse.commandQueue.isNotEmpty())
            assert(registrationResponse.resultQueue.isNotEmpty())

            val db: Dao by inject()
            val client = db.getClient(clientName)
            assertNotNull(client)
            assertEquals(registrationResponse.commandQueue, client.queueUrl)
        }
    }
}
