package au.com.shiftyjelly.pocketcasts.settings.notifications.di

import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class NotificationSettingsModule {

    @Binds
    internal abstract fun bindRepository(impl: NotificationsPreferencesRepositoryImpl): NotificationsPreferenceRepository
}
