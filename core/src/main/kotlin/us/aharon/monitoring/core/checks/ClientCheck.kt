package us.aharon.monitoring.core.checks

import us.aharon.monitoring.core.handlers.Handler


data class ClientCheck(
        override val interval: Int,
        override val notification: String,
        override val handlers: List<Handler> = emptyList(),
        override val highFlapThreshold: Int,
        override val lowFlapThreshold: Int,
        override val additional: Map<String, String?> = emptyMap(),
        override val contacts: List<String> = emptyList(),
        override val timeout: Int = 30,
        override val autoResolve: Boolean = true,
        override val occurrences: Int = 1,
        override val subdue: String = "",
        /**
         * Check command to run.
         */
        val command: String,
        /**
         * Tags used to route checks to the appropriate clients.
         */
        val subscribers: List<String>,
        /**
         * This check is not triggered by the monitoring application.
         * The check results are reported asynchronously.
         */
        val volatile: Boolean = false,
        /**
         * Time to live in seconds before the check is automatically deleted from the queue.
         */
        val ttl: Int = 90
) : Check
