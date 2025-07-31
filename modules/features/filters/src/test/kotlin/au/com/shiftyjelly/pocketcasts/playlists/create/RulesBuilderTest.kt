package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.navigation.NavDeepLinkRequest
import au.com.shiftyjelly.pocketcasts.playlists.create.CreatePlaylistViewModel.RulesBuilder
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class RulesBuilderTest {
    @Test
    fun `change min episode duration`() {
        var builder = RulesBuilder.Empty.copy(
            minEpisodeDuration = 15.minutes,
            maxEpisodeDuration = 20.minutes,
        )

        builder = builder.incrementMinDuration()
        assertEquals(15.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(15.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(4.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(4.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(3.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(2.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(1.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(0.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(0.minutes, builder.minEpisodeDuration)
    }

    @Test
    fun `change max episode duration`() {
        var builder = RulesBuilder.Empty.copy(
            minEpisodeDuration = 0.minutes,
            maxEpisodeDuration = 1.minutes,
        )

        builder = builder.decrementMaxDuration()
        assertEquals(1.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(2.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(1.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(2.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(3.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(4.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(4.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(10.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(10.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(15.minutes, builder.maxEpisodeDuration)
    }
}
