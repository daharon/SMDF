/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


class CheckGroup(val name: String) {

    val checks: MutableList<Check> = mutableListOf()
}
