package au.com.shiftyjelly.pocketcasts.coroutines.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WearIoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WearDefaultDispatcher
