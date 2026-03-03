package au.com.shiftyjelly.pocketcasts.coroutines.di

import javax.inject.Qualifier

/**
 * Annotation for providing coroutine scope that lasts the entire time the application
 * is running.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
