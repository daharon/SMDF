package us.aharon.monitoring.core.checks

import us.aharon.monitoring.core.handlers.Handler


/**
 * AWS Lambda check.
 *
 * A check that will be executed as an AWS Lambda function, independent of clients.
 */
class ServerlessCheck : Check {
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
    val command: Any = Unit // TODO: Create an interface for user implemented check classes.


    override fun run() {
        TODO("Invoke the configured check as an AWS Lambda function.")
    }
}
