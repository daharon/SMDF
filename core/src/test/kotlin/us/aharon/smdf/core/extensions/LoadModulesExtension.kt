/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.extensions

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.test.KoinTest

import us.aharon.smdf.core.di.modules


class LoadModulesExtension : KoinTest,
        BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        startKoin {
            modules(modules)
            logger(PrintLogger(Level.DEBUG))
        }
    }

    override fun afterAll(context: ExtensionContext) = stopKoin()
}
