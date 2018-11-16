/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example

import us.aharon.monitoring.core.api.Application
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.mutators.Mutator

import us.aharon.monitoring.example.checks.SYSTEM_CHECKS
import us.aharon.monitoring.example.checks.TEST_CLIENT_CHECKS


class App : Application() {

    override val checks = listOf(
            SYSTEM_CHECKS,
            TEST_CLIENT_CHECKS
    )
    override val filters = emptyList<Filter>()
    override val mutators = emptyList<Mutator>()


    companion object {
        /**
         * CLI entry point.
         */
        @JvmStatic
        fun main(vararg args: String) = App().run(args)
    }
}
