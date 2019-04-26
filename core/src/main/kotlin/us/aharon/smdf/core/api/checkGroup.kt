/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.api

import us.aharon.smdf.core.checks.CheckDslMarker
import us.aharon.smdf.core.checks.CheckGroup


@CheckDslMarker
inline fun checks(name: String, block: CheckGroup.() -> Unit): CheckGroup =
        CheckGroup(name).apply(block)

