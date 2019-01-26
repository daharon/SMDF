/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.serverlesscheckprocessor

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.*


class OkExecutor : ServerlessExecutor() {
    override val permissions: List<Permission> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result =
            Ok("This check passed")
}

class WarningExecutor : ServerlessExecutor() {
    override val permissions: List<Permission> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result =
            Warning("This check is in a WARNING state")
}

class CriticalExecutor : ServerlessExecutor() {
    override val permissions: List<Permission> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result =
            Critical("This check is in a CRITICAL state")
}

class UnknownExecutor : ServerlessExecutor() {
    override val permissions: List<Permission> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result =
            Unknown("This check is in an UNKNOWN state")
}

class ExceptionExecutor : ServerlessExecutor() {
    override val permissions: List<Permission> = emptyList()
    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result =
            throw Exception("This check throws an exception")
}
