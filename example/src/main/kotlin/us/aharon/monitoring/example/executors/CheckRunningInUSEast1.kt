/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.executors

import com.amazonaws.regions.Regions

import us.aharon.monitoring.core.checks.ServerlessCheck
import us.aharon.monitoring.core.checks.ServerlessExecutor


/**
 * Example of a Serverless Check Executor
 * Verifies that the serverless check is running in AWS availability zone us-east-1.
 */
class CheckRunningInUSEast1 : ServerlessExecutor() {

    override fun run(data: ServerlessCheck) {
        val currentRegion = System.getenv("AWS_REGION")
        if (currentRegion == Regions.US_EAST_1.name) {
            println("We're in the correct region!")
        } else {
            throw Exception("We're in $currentRegion, which is not the correct region!")
        }
    }
}
