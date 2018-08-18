/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.events


/**
 * Custom Lambda event for running a notification handler.
 */
data class NotificationEvent(
        /**
         * The full canonical name of the [NotificationHandler] class to invoke.
         */
        val notificationHandler: String?,
        val metadata: Map<String, Any?>?
)
