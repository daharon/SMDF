/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import com.amazonaws.services.lambda.runtime.Context


class FailServerlessExecutor : ServerlessExecutor() {

    override val policies: List<String> = emptyList()


    override fun run(check: ServerlessCheck, ctx: Context): Result {
        throw NotImplementedError("You must create an implementation of the ServerlessExecutor class and override its run() method.")
    }
}
