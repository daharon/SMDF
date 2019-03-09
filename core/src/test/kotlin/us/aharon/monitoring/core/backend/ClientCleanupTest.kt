/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import cloud.localstack.LocalstackExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions

import us.aharon.monitoring.core.extensions.ClientCheckTopicExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(ClientCheckTopicExtension::class))
class ClientCleanupTest {

    /**
     * Test duplicated in [DatabaseStreamProcessorTest.ClientEvents.`Client record deleted`].
     */
    @Disabled
    @Test
    fun `Client record deleted`() { }
}
