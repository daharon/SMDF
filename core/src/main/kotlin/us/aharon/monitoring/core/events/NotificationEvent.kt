/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.events

import us.aharon.monitoring.core.db.CheckResultRecord


/**
 * Custom Lambda event for running a notification handler.
 */
internal data class NotificationEvent(
        /**
         * The full canonical name of the [NotificationHandler] class to invoke.
         */
        val handler: String?,
        val checkResult: CheckResultRecord?
)
