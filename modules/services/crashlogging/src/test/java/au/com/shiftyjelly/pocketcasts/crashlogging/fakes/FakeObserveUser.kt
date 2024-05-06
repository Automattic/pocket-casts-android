package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import au.com.shiftyjelly.pocketcasts.crashlogging.ObserveUser
import au.com.shiftyjelly.pocketcasts.crashlogging.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeObserveUser : ObserveUser {
    var user: User? = null

    override fun invoke(): Flow<User?> = flowOf(user)
}
