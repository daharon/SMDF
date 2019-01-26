/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.checks


data class Permission(
        /**
         * IAM policy actions.
         * Describes the specific actions that will be allowed.
         *
         * Example:
         * <pre>{@code
         *     listOf("ses:SendEmail")
         *     listOf("sns:Publish", "sqs:SendMessage")
         * }</pre>
         */
        val actions: List<String>,

        /**
         * IAM policy resources.
         * Specifies the objects that the statement covers.
         *
         * Example:
         * <pre>{@code
         *     listOf("arn:aws:ses:us-east-1:123456789012:identity/sender@example.net")
         *     listOf("arn:aws:sns:*:*:topicname",
         *            "arn:aws:sqs:*:*:queuename")
         * }</pre>
         */
        val resources: List<String>
)
