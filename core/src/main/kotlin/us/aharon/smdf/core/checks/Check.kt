/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.checks

import us.aharon.smdf.core.handlers.NotificationHandler

import kotlin.reflect.KClass


interface Check {

    val name: String
    /**
     * Check interval in minutes.
     */
    var interval: Int
    var notification: String
    var handlers: List<KClass<out NotificationHandler>>
    // TODO:  Implement flapping detection.
    var highFlapThreshold: Int
    var lowFlapThreshold: Int
    var additional: Map<String, String?>
    var contacts: List<String>
    /**
     * Check timeout in seconds.
     */
    var timeout: Int
    /**
     * Fire the notification handlers after N consecutive occurrences in a non-OK state.
     * Not yet implemented.
     */
    // TODO:  Implement occurrences notification handling.
    var occurrences: Int
    /**
     * Perform check only if the given function evaluates to `true`.
     * eg. Perform the check only during a given time-frame.
     */
    var onlyIf: () -> Boolean
    /**
     * Perform check only if the given function evaluates to `false`.
     * eg. Perform the check only during a given time-frame.
     */
    var notIf: () -> Boolean
}
