/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.serverlesscheckprocessor

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.*


class OkExecutor : ServerlessExecutor() {
    override val policies: List<String> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context): Result =
            Ok("This check passed")
}

class WarningExecutor : ServerlessExecutor() {
    override val policies: List<String> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context): Result =
            Warning("This check is in a WARNING state")
}

class CriticalExecutor : ServerlessExecutor() {
    override val policies: List<String> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context): Result =
            Critical("This check is in a CRITICAL state")
}

class UnknownExecutor : ServerlessExecutor() {
    override val policies: List<String> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context): Result =
            Unknown("This check is in an UNKNOWN state")
}

class ExceptionExecutor : ServerlessExecutor() {
    override val policies: List<String> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context): Result =
            throw Exception("This check throws an exception")
}
