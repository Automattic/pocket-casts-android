package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSessionSourceTest {
    @Test
    fun `Android Auto controller maps to Android Auto source`() {
        val source = resolveMediaSessionSource(
            controllerPackageName = "com.google.android.projection.gearhead",
            isAutomotive = false,
            default = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
        )

        assertEquals(SourceView.ANDROID_AUTO, source)
    }

    @Test
    fun `other controller keeps default source`() {
        val source = resolveMediaSessionSource(
            controllerPackageName = "com.example.controller",
            isAutomotive = false,
            default = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
        )

        assertEquals(SourceView.MEDIA_BUTTON_BROADCAST_ACTION, source)
    }

    @Test
    fun `missing controller keeps default source`() {
        val source = resolveMediaSessionSource(
            controllerPackageName = null,
            isAutomotive = false,
            default = SourceView.UNKNOWN,
        )

        assertEquals(SourceView.UNKNOWN, source)
    }

    @Test
    fun `Automotive maps any controller to Automotive source`() {
        val source = resolveMediaSessionSource(
            controllerPackageName = "com.android.car.media",
            isAutomotive = true,
            default = SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION,
        )

        assertEquals(SourceView.AUTOMOTIVE, source)
    }
}
