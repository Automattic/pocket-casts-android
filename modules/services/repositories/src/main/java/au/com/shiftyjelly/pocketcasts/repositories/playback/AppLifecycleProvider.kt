package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlinx.coroutines.flow.StateFlow

interface AppLifecycleProvider {
    val isInForeground: StateFlow<Boolean>
}
