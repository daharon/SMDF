/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import cloud.localstack.LocalstackExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.smdf.core.backend.messages.ClientDeregistrationRequest
import us.aharon.smdf.core.db.ClientRecord
import us.aharon.smdf.core.db.Dao
import us.aharon.smdf.core.extensions.DynamoDBTableExtension
import us.aharon.smdf.core.extensions.LoadModulesExtension

import kotlin.test.assertFalse
import kotlin.test.assertNotNull


class ClientDeregistrationTest {

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class Success : KoinTest {

        @Test
        fun `De-activate client`() {
            val db: Dao by inject()
            val clientName = "test-client"
            // Pre-populate the database with the client.
            db.saveClient(ClientRecord(clientName, emptyList(), active = true), "Created")

            val deregistrationRequest = ClientDeregistrationRequest(clientName)
            val deregisterResponse = ClientDeregistration().run(deregistrationRequest)
            assert(deregisterResponse.code == 200)

            // Read the updated client from the database.
            val client = db.getClient(clientName)
            assertNotNull(client)
            assertFalse(client.active!!)
        }
    }

    @Nested
    @Extensions(
        ExtendWith(LocalstackExtension::class),
        ExtendWith(LoadModulesExtension::class),
        ExtendWith(DynamoDBTableExtension::class))
    inner class Failure : KoinTest {

        @Test
        fun `Empty client name`() {
            val clientName = ""
            val deregistrationRequest = ClientDeregistrationRequest(clientName)
            val deregisterResponse = ClientDeregistration().run(deregistrationRequest)
            assert(deregisterResponse.code == 400)
        }

        @Test
        fun `NULL client name`() {
            val clientName: String? = null
            val deregistrationRequest = ClientDeregistrationRequest(clientName)
            val deregisterResponse = ClientDeregistration().run(deregistrationRequest)
            assert(deregisterResponse.code == 400)
        }

        @Test
        fun `Client not found`() {
            val clientName = "test-client"
            // The database is NOT pre-populated with the client.
            val deregistrationRequest = ClientDeregistrationRequest(clientName)
            val deregisterResponse = ClientDeregistration().run(deregistrationRequest)
            assert(deregisterResponse.code == 404)
        }
    }
}
