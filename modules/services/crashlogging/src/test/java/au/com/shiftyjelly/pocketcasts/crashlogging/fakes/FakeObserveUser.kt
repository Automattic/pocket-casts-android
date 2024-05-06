package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import au.com.shiftyjelly.pocketcasts.crashlogging.ObserveUser
import au.com.shiftyjelly.pocketcasts.crashlogging.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeObserveUser : ObserveUser {
    override fun invoke(): Flow<User?> = emptyFlow()
}
