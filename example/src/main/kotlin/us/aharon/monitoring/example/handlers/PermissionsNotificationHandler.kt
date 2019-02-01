/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.example.handlers

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.checks.Permission
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.handlers.NotificationHandler


/**
 * Example notification handler that sets IAM policies/permissions.
 */
class PermissionsNotificationHandler : NotificationHandler() {

    override val permissions: List<Permission> = listOf(
            Permission(
                    actions = listOf("ses:SendEmail"),
                    resources = listOf("arn:aws:ses:*:*:identity/example.com"))
    )

    override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context, credentials: AWSCredentialsProvider) {
        ctx.logger.log("This notification handler is using the following credentials:  $credentials")
    }
}
