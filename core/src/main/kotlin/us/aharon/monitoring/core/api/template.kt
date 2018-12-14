/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.api

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.checks.ClientCheck
import us.aharon.monitoring.core.checks.ServerlessCheck


typealias ClientCheckTemplate = CheckGroup.(name: String, block: ClientCheck.() -> Unit) -> Unit
typealias ServerlessCheckTemplate = CheckGroup.(name: String, block: ServerlessCheck.() -> Unit) -> Unit

fun clientCheckTemplate(defaults: ClientCheck.() -> Unit): ClientCheckTemplate =
        fun CheckGroup.(name: String, block: ClientCheck.() -> Unit) {
            val check = ClientCheck(name)
            check.apply(defaults)
            check.apply(block)
            checks.add(check)
        }

fun serverlessCheckTemplate(defaults: ServerlessCheck.() -> Unit): ServerlessCheckTemplate =
        fun CheckGroup.(name: String, block: ServerlessCheck.() -> Unit) {
            val check = ServerlessCheck(name)
            check.apply(defaults)
            check.apply(block)
            checks.add(check)
        }
