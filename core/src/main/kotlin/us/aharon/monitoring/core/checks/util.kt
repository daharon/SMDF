/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


internal fun List<CheckGroup>.getCheck(group: String, name: String): Check = this.find {
    it.name == group
}?.checks?.find {
    it.name == name
}!!
