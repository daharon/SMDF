/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.handlers

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.filters.Filter
import us.aharon.monitoring.core.mutators.Mutator


/**
 * Abstract base class for implementing custom notification handlers.
 *
 * - Run filters and mutators?
 * - Assume role.
 * - Pass event to the run method.
 */
abstract class NotificationHandler {

    /*  Are these actually going to be useful?
    abstract val filters: List<Filter>
    abstract val mutators: List<Mutator>
    */
    /**
     * Specify IAM policies which this handler requires.
     *
     * - Policy documents.
     * - Policy ARNs.
     */
    abstract val policies: List<String>


    /**
     * Implement this function.
     */
    abstract fun run(check: Check, checkResult: CheckResultRecord, ctx: Context)

    /**
     * Wrapper for the [run] function.
     */
    fun execute(check: Check, checkResult: CheckResultRecord, ctx: Context) {
        // STS AssumeRole

        // Run handler implementation.
        this.run(check, checkResult, ctx)
    }
}
