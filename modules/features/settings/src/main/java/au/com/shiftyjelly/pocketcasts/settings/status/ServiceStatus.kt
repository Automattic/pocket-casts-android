package au.com.shiftyjelly.pocketcasts.settings.status

sealed class ServiceStatus {
    object Queued : ServiceStatus() {
        override fun toString(): String {
            return "Queued"
        }
    }
    object Running : ServiceStatus() {
        override fun toString(): String {
            return "Running"
        }
    }
    object Success : ServiceStatus() {
        override fun toString(): String {
            return "Success"
        }
    }
    data class Failed(val userMessage: String?, val log: String) : ServiceStatus()
}
