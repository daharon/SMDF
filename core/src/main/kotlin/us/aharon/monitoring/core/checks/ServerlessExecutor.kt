/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import com.amazonaws.auth.AWSCredentialsProvider
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
     * Perform the desired check operation and return with a [Result].
     *
     * @param check The [ServerlessCheck] which triggered this execution.
     * @param ctx The [Context] as provided by the AWS Lambda runtime.
     * @param credentials AWS credentials which have the permissions as defined in the [permissions] list of this class.
     *
     * @return One of the following:  [Ok], [Warning], [Critical], [Unknown]
     */
    abstract fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result

    /**
     * Wrapper for the [run] function.
     */
    internal fun execute(check: ServerlessCheck, context: Context, credentials: AWSCredentialsProvider): Result = try {
        // TODO: This try-catch block duplicates functionality in the ServerlessCheckProcessor.
        this.run(check, context, credentials)
    } catch (e: Exception) {
        Critical(e.message ?: e.toString())
    }
}
