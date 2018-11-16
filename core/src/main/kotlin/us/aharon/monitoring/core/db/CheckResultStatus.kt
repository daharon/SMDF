/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db


enum class CheckResultStatus(name: String) {
    UNKNOWN("UNKNOWN"),
    CRITICAL("CRITICAL"),
    WARNING("WARNING"),
    OK("OK")
}
