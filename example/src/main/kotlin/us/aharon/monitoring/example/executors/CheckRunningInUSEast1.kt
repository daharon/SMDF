/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.executors

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.*


/**
 * Example of a Serverless Check Executor.
 *
 * Verifies that the serverless check is running in AWS availability zone us-east-1.
 */
class CheckRunningInUSEast1 : ServerlessExecutor() {

    override val permissions: List<Permission> = emptyList()


    override fun run(check: ServerlessCheck, ctx: Context): Result {
        // This environment variable is provided by AWS Lambda.
        // https://docs.aws.amazon.com/lambda/latest/dg/current-supported-versions.html
        val currentRegion = System.getenv("AWS_REGION")

        if (currentRegion.toLowerCase() == Regions.US_EAST_1.name.toLowerCase()) {
            return Ok("We're in $currentRegion, the correct region!")
        }
        return Critical("We're in $currentRegion, which is not the correct region!")
    }
}
