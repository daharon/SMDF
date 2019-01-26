/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


/**
 * Find the [Check][us.aharon.monitoring.core.checks.Check] instance given its group and name.
 */
internal fun List<CheckGroup>.getCheck(group: String, name: String): Check = this.find {
    it.name == group
}?.checks?.find {
    it.name == name
}!!

/**
 * The Parameter Store path that contains the ARNs which correspond to
 * serverless checks.
 */
internal fun serverlessExecutorParameterPath(environment: String): String =
        "/$environment/monitoring/role"
