package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import kotlinx.coroutines.flow.StateFlow

interface AudioRouteMonitor {
    val route: StateFlow<AudioRoute>
}
