package au.com.shiftyjelly.pocketcasts.coroutines.di

import javax.inject.Qualifier

/**
 * Qualifier for Dispatchers.Main
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Qualifier for Dispatchers.IO
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier for Dispatchers.Default
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
