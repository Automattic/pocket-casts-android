package au.com.shiftyjelly.pocketcasts.repositories.di

import javax.inject.Qualifier

/**
 * Annotation for providing lifecycle owner that is associated with the app's process.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProcessLifecycle
