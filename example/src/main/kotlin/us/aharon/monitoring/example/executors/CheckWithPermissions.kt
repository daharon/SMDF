/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.example.executors

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.*


/**
 * Example serverless check executor with IAM permissions.
 */
class CheckWithPermissions : ServerlessExecutor() {

    override val permissions: List<Permission> = listOf(
            Permission(
                    actions = listOf("iam:ListUsers"),
                    resources = listOf("*"))
    )

    override fun run(check: ServerlessCheck, ctx: Context): Result = Ok(output = "All OK")
}
