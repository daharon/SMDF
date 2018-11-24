/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import kotlin.reflect.KClass

import us.aharon.monitoring.core.handlers.NotificationHandler


/**
 * Client-side check.
 *
 * A check that will be pushed to the queue for a client-side subscriber to execute remotely.
 */
class ClientCheck(override val name: String) : Check {
    override var interval: Int = 10
    override var notification: String = "Notification not set"
    override var handlers: List<KClass<out NotificationHandler>> = emptyList()
    override var highFlapThreshold: Int = 70  // TODO: Find appropriate default value.
    override var lowFlapThreshold: Int = 30  // TODO: Find appropriate default value.
    override var additional: Map<String, String?> = emptyMap()
    override var contacts: List<String> = emptyList()
    override var timeout: Int = 30
    override var autoResolve: Boolean = true
    override var occurrences: Int = 1
    override var subdue: String = ""
    /**
     * Check command to run.
     * Default value forces a CRITICAL state.
     */
    var command: String = "exit 2"
    /**
     * Tags used to route checks to the appropriate clients.
     */
    var subscribers: List<String> = emptyList()
    /**
     * This check is not triggered by the monitoring application.
     * The check results are reported asynchronously.
     */
    var volatile: Boolean = false
    /**
     * Time to live in seconds before the check is automatically deleted from the queue.
     */
    var ttl: Int = 90
}
