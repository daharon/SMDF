/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.api

import us.aharon.smdf.core.checks.CheckDslMarker
import us.aharon.smdf.core.checks.CheckGroup
import us.aharon.smdf.core.checks.ClientCheck
import us.aharon.smdf.core.checks.ServerlessCheck


@CheckDslMarker
inline fun CheckGroup.check(name: String, block: ClientCheck.() -> Unit) {
    val check = ClientCheck(name)
    check.apply(block)
    checks.add(check)
}

@CheckDslMarker
inline fun CheckGroup.serverlessCheck(name: String, block: ServerlessCheck.() -> Unit) {
    val check = ServerlessCheck(name)
    check.apply(block)
    checks.add(check)
}
