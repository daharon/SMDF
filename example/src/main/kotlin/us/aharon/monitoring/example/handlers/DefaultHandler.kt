/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.example.handlers

import com.amazonaws.services.lambda.runtime.Context

import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.handlers.NotificationHandler


/**
 * https://docs.sensu.io/sensu-core/1.4/reference/handlers/#handler-configuration
 */
class DefaultHandler : NotificationHandler() {

    override val policies: List<String> = emptyList()


    override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context) {
        ctx.logger.log("${this::class.qualifiedName} received the following check result:  $checkResult")
    }
}
