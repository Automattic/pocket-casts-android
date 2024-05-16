package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import com.automattic.encryptedlogging.EncryptedLogging
import com.automattic.encryptedlogging.store.OnEncryptedLogUploaded
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FakeEncryptedLogging : EncryptedLogging {
    override fun enqueueSendingEncryptedLogs(
        uuid: String,
        file: File,
        shouldUploadImmediately: Boolean,
    ) = Unit

    override fun observeEncryptedLogsUploadResult(): StateFlow<OnEncryptedLogUploaded?> = MutableStateFlow(null)

    override fun resetUploadStates() = Unit

    override suspend fun uploadEncryptedLogs() = Unit
}
