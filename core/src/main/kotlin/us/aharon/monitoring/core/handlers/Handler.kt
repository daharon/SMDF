/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.handlers

import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.mutators.Mutator


interface Handler {

    val filters: List<Filter>
    val mutators: List<Mutator>

    fun run(event: Event)
}
