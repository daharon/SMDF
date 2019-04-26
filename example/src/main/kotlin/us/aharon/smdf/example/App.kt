/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.example

import us.aharon.smdf.core.api.Application
import us.aharon.smdf.core.filters.Filter
import us.aharon.smdf.core.mutators.Mutator

import us.aharon.smdf.example.checks.SYSTEM_CHECKS
import us.aharon.smdf.example.checks.TEST_CLIENT_CHECKS


class App : Application() {

    override val filters = emptyList<Filter>()
    override val mutators = emptyList<Mutator>()
    override val checks = listOf(
            SYSTEM_CHECKS,
            TEST_CLIENT_CHECKS
    )
}

fun main(vararg args: String) = App().run(args)
