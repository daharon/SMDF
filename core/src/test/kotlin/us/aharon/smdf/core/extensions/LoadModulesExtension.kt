/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.extensions

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.test.KoinTest

import us.aharon.smdf.core.di.modules


class LoadModulesExtension : KoinTest,
        BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        startKoin(listOf(modules), logger = PrintLogger())
    }

    override fun afterAll(context: ExtensionContext) {
        stopKoin()
    }
}
