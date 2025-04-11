package au.com.shiftyjelly.pocketcasts.repositories.discover

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DiscoverDeepLinkNavigation @Inject constructor() {
    private val _pendingDestination = MutableStateFlow<Destination?>(null)
    val pendingDestination = _pendingDestination.asStateFlow()

    fun navigateTo(destination: Destination) {
        _pendingDestination.value = destination
    }

    sealed class Destination {
        object StaffPicks : Destination()
        object Recommendations : Destination()
        object Trending : Destination()
    }
}
