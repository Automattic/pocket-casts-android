package au.com.shiftyjelly.pocketcasts.repositories.jobs

import androidx.test.ext.junit.runners.AndroidJUnit4
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class VersionMigrationsJobTest {

    @Test
    fun testUpgradeMultiSelectItems() {
        val resourceIds = listOf("2131362851", "2131362827", "2131362859", "2131362857", "2131362838", "2131362836", "2131362843")
        val expectedItems = listOf("play_last", "play_next", "star", "share", "download", "archive", "mark_as_played")

        val settings = mock<Settings>()
        whenever(settings.getMultiSelectItems()).thenReturn(resourceIds)

        VersionMigrationsJob.upgradeMultiSelectItems(settings)

        verify(settings).setMultiSelectItems(expectedItems)
    }

    @Test
    fun testUpgradeMultiSelectItemsNotRequired() {
        val items = listOf("play_last", "play_next", "star", "share", "download", "archive", "mark_as_played")

        val settings = mock<Settings>()
        whenever(settings.getMultiSelectItems()).thenReturn(items)

        VersionMigrationsJob.upgradeMultiSelectItems(settings)

        verify(settings, never()).setMultiSelectItems(items)
    }
}
