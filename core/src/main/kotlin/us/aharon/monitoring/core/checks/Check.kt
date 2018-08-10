package us.aharon.monitoring.core.checks

import us.aharon.monitoring.core.handlers.Handler


interface Check {
    /**
     * Check interval in minutes.
     */
    val interval: Int
    val notification: String
    val handlers: List<Handler>
    val highFlapThreshold: Int
    val lowFlapThreshold: Int
    val additional: Map<String, String?>
    val contacts: List<String>
    /**
     * Check timeout in seconds.
     */
    val timeout: Int
    /**
     * Resolve the check state automatically on a state transition of WARNING/CRITICAL to OK.
     * Non-MVP feature.
     */
    val autoResolve: Boolean
    /**
     * Fire the notification handlers after N consecutive occurrences in a non-OK state.
     */
    val occurrences: Int
    /**
     * Do not perform the check during the provided time-frame.
     * TODO: Come up with a data structure for this.
     */
    val subdue: String
}
