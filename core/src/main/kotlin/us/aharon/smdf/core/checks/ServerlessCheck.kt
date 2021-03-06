/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.checks

import kotlin.reflect.KClass

import us.aharon.smdf.core.handlers.NotificationHandler


/**
 * AWS Lambda check.
 *
 * A check that will be executed as an AWS Lambda function, independent of clients.
 */
class ServerlessCheck(override val name: String) : Check {
    override var interval: Int = 10
    override var notification: String = "Notification not set"
    override var handlers: List<KClass<out NotificationHandler>> = emptyList()
    override var highFlapThreshold: Int = 70  // TODO: Find appropriate default value.
    override var lowFlapThreshold: Int = 30  // TODO: Find appropriate default value.
    override var additional: Map<String, String?> = emptyMap()
    override var contacts: List<String> = emptyList()
    override var timeout: Int = 30
    override var occurrences: Int = 1
    override var onlyIf: () -> Boolean = { true }
    override var notIf: () -> Boolean = { false }

    /**
     * ServerlessCheck class to run.
     * An implementation of the [ServerlessExecutor] abstract class.
     */
    var executor: KClass<out ServerlessExecutor> = FailServerlessExecutor::class
}
