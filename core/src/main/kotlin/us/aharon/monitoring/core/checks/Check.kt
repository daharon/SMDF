/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import kotlin.reflect.KClass

import us.aharon.monitoring.core.handlers.Handler


interface Check {

    val name: String
    /**
     * Check interval in minutes.
     */
    var interval: Int
    var notification: String
    var handlers: List<KClass<out Handler>>
    var highFlapThreshold: Int
    var lowFlapThreshold: Int
    var additional: Map<String, String?>
    var contacts: List<String>
    /**
     * Check timeout in seconds.
     */
    var timeout: Int
    /**
     * Resolve the check state automatically on a state transition of WARNING/CRITICAL to OK.
     * Non-MVP feature.
     */
    var autoResolve: Boolean
    /**
     * Fire the notification handlers after N consecutive occurrences in a non-OK state.
     */
    var occurrences: Int
    /**
     * Do not perform the check during the provided time-frame.
     * TODO: Come up with a data structure for this.
     */
    var subdue: String

    /**
     * Execute the check operation.
     */
    fun run()
}
