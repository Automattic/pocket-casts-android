package au.com.shiftyjelly.pocketcasts.settings.advanced

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.Moshi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdvancedSettingsTest {
    private lateinit var settings: Settings

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileName = "FILE_NAME"

        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        val moshi = Moshi.Builder().build()
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        settings = SettingsImpl(
            sharedPreferences = sharedPreferences,
            privatePreferences = sharedPreferences,
            context = context,
            firebaseRemoteConfig = firebaseRemoteConfig,
            moshi = moshi,
            bookmarkFeature = BookmarkFeatureControl(),
        )
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileName = "FILE_NAME"

        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun verifyDefaultSettings() {
        // Non-advanced settings
        assertEquals(false, settings.warnOnMeteredNetwork.flow.value)
        assertEquals(true, settings.autoDownloadUnmeteredOnly.flow.value)
        assertEquals(false, settings.autoDownloadOnlyWhenCharging.flow.value)
        assertEquals(false, settings.autoDownloadUpNext.flow.value)
        assertEquals(true, settings.backgroundRefreshPodcasts.flow.value)

        // Advanced settings
        assertEquals(true, settings.syncOnMeteredNetwork())
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = false))
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = true))
        assertEquals(NetworkType.CONNECTED, settings.getWorkManagerNetworkTypeConstraint())

        // Now modify the default and check the settings
        settings.setSyncOnMeteredNetwork(false)
        assertEquals(false, settings.syncOnMeteredNetwork())
        assertEquals(false, settings.refreshPodcastsOnResume(isUnmetered = false))
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = true))
        assertEquals(NetworkType.UNMETERED, settings.getWorkManagerNetworkTypeConstraint())
    }

    @Test
    fun shelfItemsByDefaultReturnAllEntries() {
        assertTrue(settings.shelfItems.value.containsAll(ShelfItem.entries))
    }

    @Test
    fun shelfItemsSettingsAlwaysSavesAllEntries() {
        settings.shelfItems.set(listOf(ShelfItem.Archive), updateModifiedAt = false)

        val items = settings.shelfItems.value

        assertTrue(items.containsAll(ShelfItem.entries))
    }

    @Test
    fun shelfItemsDoNotSaveDuplicates() {
        settings.shelfItems.set(listOf(ShelfItem.Cast, ShelfItem.Cast), updateModifiedAt = false)

        val items = settings.shelfItems.value

        assertEquals(items.filter { it == ShelfItem.Cast }, listOf(ShelfItem.Cast))
    }

    @Test
    fun shelfItemsAreSavedInOrder() {
        val items = listOf(ShelfItem.Archive, ShelfItem.Bookmark, ShelfItem.Star, ShelfItem.Played, ShelfItem.Sleep)

        settings.shelfItems.set(items, updateModifiedAt = false)

        assertEquals(settings.shelfItems.value.take(5), items)
    }
}
