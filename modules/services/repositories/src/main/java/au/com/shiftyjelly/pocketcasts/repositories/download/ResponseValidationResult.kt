package au.com.shiftyjelly.pocketcasts.repositories.download

data class ResponseValidationResult(
    var errorMessage: String? = null,
    var isValid: Boolean = false,
    var isAlternateUrlFound: Boolean = false
)
