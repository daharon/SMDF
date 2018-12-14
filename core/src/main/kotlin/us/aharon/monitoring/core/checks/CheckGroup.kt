/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


@DslMarker
annotation class CheckDslMarker


@CheckDslMarker
class CheckGroup(val name: String) {

    val checks: MutableList<Check> = mutableListOf()
}
