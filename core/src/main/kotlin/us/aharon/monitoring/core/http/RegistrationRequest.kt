package us.aharon.monitoring.core.http


data class RegistrationRequest(
        val clientName: String?,
        val clientTags: List<String>?
)
