/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend.notificationprocessor

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.handlers.NotificationHandler


/**
 * Notification handler for use in testing.
 */
class TestNotificationHandler : NotificationHandler() {
    override val policies: List<String> = emptyList()

    override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context) {
        println("Running notification for:")
        println("Check:  $check")
        println("Check Result:  $checkResult")
    }
}
