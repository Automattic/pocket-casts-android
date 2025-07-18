package au.com.shiftyjelly.pocketcasts.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.encryptedlogging.EncryptedLogging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitializeRemoteLogging @Inject constructor(
    private val crashLogging: CrashLogging,
    private val encryptedLogging: EncryptedLogging,
) {
    operator fun invoke() {
        crashLogging.initialize()
        encryptedLogging.resetUploadStates()
        encryptedLogging.uploadEncryptedLogs()
    }
}
