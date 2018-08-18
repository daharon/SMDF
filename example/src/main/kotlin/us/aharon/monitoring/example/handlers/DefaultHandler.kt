/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.handlers

import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.handlers.Event
import us.aharon.monitoring.core.handlers.Handler
import us.aharon.monitoring.core.mutators.Mutator


/**
 * https://docs.sensu.io/sensu-core/1.4/reference/handlers/#handler-configuration
 */
class DefaultHandler : Handler {

    override val filters = listOf<Filter>(
//            ::exampleFilter
    )
    override val mutators = listOf<Mutator>(
//            ::exampleMutator
    )

    override fun run(event: Event) {
        TODO("Handler action")
    }
}
