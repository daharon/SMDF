/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.common

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context

import us.aharon.smdf.core.checks.Check
import us.aharon.smdf.core.checks.Permission
import us.aharon.smdf.core.db.CheckResultRecord
import us.aharon.smdf.core.handlers.NotificationHandler


/**
 * Notification handler for use in testing.
 */
class TestNotificationHandler : NotificationHandler() {

    override val permissions: List<Permission> = emptyList()

    override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context, credentials: AWSCredentialsProvider) {
        println("Running notification for:")
        println("Check:  $check")
        println("Check Result:  $checkResult")
        println("Credentials: $credentials")
    }
}
