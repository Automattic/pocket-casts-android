package au.com.shiftyjelly.pocketcasts.ui.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SharedImageLoader

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WearImageLoader
