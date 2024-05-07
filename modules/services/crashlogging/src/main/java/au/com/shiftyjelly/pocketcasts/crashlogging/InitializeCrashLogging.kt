package au.com.shiftyjelly.pocketcasts.crashlogging

import au.com.shiftyjelly.pocketcasts.crashlogging.di.ProvideApplicationScope
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.encryptedlogging.EncryptedLogging
import javax.inject.Inject
import kotlinx.coroutines.launch

class InitializeCrashLogging @Inject constructor(
    private val crashLogging: CrashLogging,
    private val encryptedLogging: EncryptedLogging,
    private val provideApplicationScope: ProvideApplicationScope,
) {

    operator fun invoke() {
        crashLogging.initialize()

        encryptedLogging.resetUploadStates()

        provideApplicationScope().launch {
            encryptedLogging.uploadEncryptedLogs()
        }
    }
}
