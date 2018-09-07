/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import kotlin.reflect.KClass

import us.aharon.monitoring.core.handlers.Handler


/**
 * AWS Lambda check.
 *
 * A check that will be executed as an AWS Lambda function, independent of clients.
 */
class ServerlessCheck(override val name: String) : Check {
    override var interval: Int = 10
    override var notification: String = "Notification not set"
    override var handlers: List<KClass<out Handler>> = emptyList()
    override var highFlapThreshold: Int = 70  // TODO: Find appropriate default value.
    override var lowFlapThreshold: Int = 30  // TODO: Find appropriate default value.
    override var additional: Map<String, String?> = emptyMap()
    override var contacts: List<String> = emptyList()
    override var timeout: Int = 30
    override var autoResolve: Boolean = true
    override var occurrences: Int = 1
    override var subdue: String = ""

    /**
     * ServerlessCheck class to run.
     */
    var executor: KClass<out ServerlessExecutor> = FailServerlessExecutor::class
}
