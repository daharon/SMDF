package us.aharon.monitoring.core.checks

import us.aharon.monitoring.core.handlers.Handler


/**
 * Client-side check.
 *
 * A check that will be pushed to the queue for a client-side subscriber to execute remotely.
 */
class ClientCheck : Check {
    override val interval: Int = 10
    override val notification: String = "Notification not set"
    override val handlers: List<Handler> = emptyList()
    override val highFlapThreshold: Int = 70  // TODO: Find appropriate default value.
    override val lowFlapThreshold: Int = 30  // TODO: Find appropriate default value.
    override val additional: Map<String, String?> = emptyMap()
    override val contacts: List<String> = emptyList()
    override val timeout: Int = 30
    override val autoResolve: Boolean = true
    override val occurrences: Int = 1
    override val subdue: String = ""
    /**
     * Check command to run.
     * Default value forces a CRITICAL state.
     */
    val command: String = "exit 2"
    /**
     * Tags used to route checks to the appropriate clients.
     */
    val subscribers: List<String> = emptyList()
    /**
     * This check is not triggered by the monitoring application.
     * The check results are reported asynchronously.
     */
    val volatile: Boolean = false
    /**
     * Time to live in seconds before the check is automatically deleted from the queue.
     */
    val ttl: Int = 90


    override fun run() {
        TODO("Push the configured check to the queue.")
    }
}
