/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.api

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.checks.ClientCheck
import us.aharon.monitoring.core.checks.ServerlessCheck


inline fun CheckGroup.check(name: String, block: ClientCheck.() -> Unit) {
    val check = ClientCheck(name)
    check.apply(block)
    checks.add(check)
}

inline fun CheckGroup.serverlessCheck(name: String, block: ServerlessCheck.() -> Unit) {
    val check = ServerlessCheck(name)
    check.apply(block)
    checks.add(check)
}
