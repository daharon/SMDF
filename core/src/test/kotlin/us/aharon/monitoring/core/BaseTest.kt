/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.test.KoinTest

import us.aharon.monitoring.core.di.modules


/**
 * Base test class that enables dependency injection.
 */
abstract class BaseTest : KoinTest {

    companion object {

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            startKoin(listOf(modules), logger = PrintLogger())
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            stopKoin()
        }
    }
}
