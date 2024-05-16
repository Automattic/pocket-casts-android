package au.com.shiftyjelly.pocketcasts.crashlogging.di

import kotlinx.coroutines.CoroutineScope

fun interface ProvideApplicationScope {
    operator fun invoke(): CoroutineScope
}
