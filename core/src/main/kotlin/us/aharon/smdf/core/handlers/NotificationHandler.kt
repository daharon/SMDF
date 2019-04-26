/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.handlers

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context

import us.aharon.smdf.core.checks.Check
import us.aharon.smdf.core.checks.Permission
import us.aharon.smdf.core.db.CheckResultRecord
import us.aharon.smdf.core.filters.Filter
import us.aharon.smdf.core.mutators.Mutator


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
    abstract val permissions: List<Permission>


    /**
     * Perform the desired notification operation.
     *
     * @param check The [Check] which triggered this execution.
     * @param checkResult The check result which triggered this execution.
     * @param ctx The [Context] as provided by the AWS Lambda runtime.
     * @param credentials AWS credentials which have the permissions as defined in the [permissions] list of this class.
     */
    abstract fun run(check: Check, checkResult: CheckResultRecord, ctx: Context, credentials: AWSCredentialsProvider)
}
