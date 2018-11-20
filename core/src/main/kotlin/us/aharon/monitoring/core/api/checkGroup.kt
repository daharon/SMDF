/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.api

import us.aharon.monitoring.core.checks.CheckGroup


inline fun checks(name: String, block: CheckGroup.() -> Unit): CheckGroup =
        CheckGroup(name).apply(block)

