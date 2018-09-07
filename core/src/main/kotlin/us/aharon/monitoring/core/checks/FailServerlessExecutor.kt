/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

class FailServerlessExecutor : ServerlessExecutor() {

    override fun run(data: ServerlessCheck) {
        throw NotImplementedError("You must create an implementation of the ServerlessExecutor class and override its run() method.")
    }
}
