package au.com.shiftyjelly.pocketcasts.crashlogging

import kotlinx.coroutines.flow.Flow

@JvmInline
value class User(val email: String)

interface ObserveUser {
    fun invoke(): Flow<User?>
}
