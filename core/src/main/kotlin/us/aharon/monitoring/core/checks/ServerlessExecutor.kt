/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.db.CheckResultStatus


/**
 * The result object which all serverless checks must return.
 */
sealed class Result {
    abstract val output: String
    abstract val status: CheckResultStatus
}
data class Ok(override val output: String) : Result() {
    override val status = CheckResultStatus.OK
}
data class Warning(override val output: String) : Result() {
    override val status = CheckResultStatus.WARNING
}
data class Critical(override val output: String) : Result() {
    override val status = CheckResultStatus.CRITICAL
}
data class Unknown(override val output: String) : Result() {
    override val status = CheckResultStatus.WARNING
}


/**
 * Abstract base class for all serverless checks.
 *
 * Implement the [run] method to perform your check.
 * Return a [Result] indicating the status of the check.
 */
abstract class ServerlessExecutor {

    /**
     * Specify IAM policy permissions which this handler requires.
     * See [Permission] class.
     */
    abstract val permissions: List<Permission>

    /**
     * Implement this function.
     */
    abstract fun run(check: ServerlessCheck, ctx: Context): Result


    /**
     * Wrapper for the [run] function.
     */
    fun execute(check: ServerlessCheck, context: Context): Result = try {
        // TODO:  Implement AssumeRole operation using the IAM Role generated to allow the policies defined in the variable above.
        this.run(check, context)
    } catch (e: Exception) {
        Critical(e.message ?: e.toString())
    }
}
