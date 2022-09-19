package au.com.shiftyjelly.pocketcasts.wear.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsEmulator

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForApplicationScope
