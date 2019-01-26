/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.example.executors

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder
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

    override fun run(check: ServerlessCheck, ctx: Context, credentials: AWSCredentialsProvider): Result {
        val iamClient = AmazonIdentityManagementClientBuilder.standard()
                .withCredentials(credentials)
                .build()
        val iamUsers = iamClient.listUsers().users
        return Ok("OK - ${iamUsers.map { it.userName }}")
    }
}
