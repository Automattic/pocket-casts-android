package au.com.shiftyjelly.pocketcasts.repositories.di

import javax.inject.Qualifier

/**
 * Annotation for providing coroutine scope that lasts the entire time the application
 * is running. Essentially, this is the better form of GlobalScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Annotation for providing lifecycle owner that is associated with the app's process.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProcessLifecycle
