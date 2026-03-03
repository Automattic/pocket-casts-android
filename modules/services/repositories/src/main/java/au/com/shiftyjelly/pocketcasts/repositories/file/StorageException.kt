package au.com.shiftyjelly.pocketcasts.repositories.file

class StorageException(
    private val detailMessage: String,
) : Exception(detailMessage) {
    override val message: String get() = detailMessage
}
