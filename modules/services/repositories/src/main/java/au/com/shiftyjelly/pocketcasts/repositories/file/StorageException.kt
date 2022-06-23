package au.com.shiftyjelly.pocketcasts.repositories.file

class StorageException : Exception {

    constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)

    constructor(detailMessage: String) : super(detailMessage)

    constructor(throwable: Throwable) : super(throwable)
}
