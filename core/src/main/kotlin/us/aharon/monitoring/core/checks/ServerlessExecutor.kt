/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


abstract class ServerlessExecutor {

    abstract fun run(data: ServerlessCheck)
}
