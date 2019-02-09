/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.example.handlers

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.*
import us.aharon.monitoring.core.checks.Check
import us.aharon.monitoring.core.checks.Permission
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.handlers.NotificationHandler


class SesNotificationHandler : NotificationHandler() {

    override val permissions: List<Permission> = listOf(
            Permission(
                    actions = listOf("ses:SendEmail"),
                    resources = listOf("arn:aws:ses:*:*:identity/example.com"))
    )

    override fun run(check: Check, checkResult: CheckResultRecord, ctx: Context, credentials: AWSCredentialsProvider) {
        val request = SendEmailRequest()
                .withSource("alert@example.com")
                .withDestination(Destination(check.contacts))
                .withMessage(Message(
                        Content("${checkResult.status} - ${checkResult.source} - ${check.notification}"),
                        Body(Content("""
                            Message:  ${check.notification}
                            Source:   ${checkResult.source}
                            Status:   ${checkResult.status}
                            Timestamp:  ${checkResult.completedAt}

                            Output:
                            ${checkResult.output}
                        """.trimIndent()))
                ))
        val client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withCredentials(credentials)
                .build()
        client.sendEmail(request)
        ctx.logger.log("Sent email.")
    }
}
