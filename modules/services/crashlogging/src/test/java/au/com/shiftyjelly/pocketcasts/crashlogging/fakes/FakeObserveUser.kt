package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import au.com.shiftyjelly.pocketcasts.crashlogging.ObserveUser
import au.com.shiftyjelly.pocketcasts.crashlogging.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeObserveUser : ObserveUser {
    private val users = MutableStateFlow<User?>(null)

    fun emitUser(user: User?) {
        users.value = user
    }

    override fun invoke(): Flow<User?> = users
}
