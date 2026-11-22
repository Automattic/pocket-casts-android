package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(UnstableApi::class)
class VideoTrackSelectionTest {

    @Test
    fun `disables video without a surface for progressive playback`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isHlsStream = false))
    }

    @Test
    fun `keeps video when a surface is attached`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = true, isHlsStream = false))
        assertFalse(
            shouldDisableVideoTrack(
                audioOnly = false,
                hasVideoSurface = true,
                isHlsStream = true,
                hasIndependentAudio = true,
            ),
        )
    }

    @Test
    fun `audio only setting always disables video`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = true, isHlsStream = false))
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = false, isHlsStream = true))
        assertTrue(
            shouldDisableVideoTrack(
                audioOnly = true,
                hasVideoSurface = true,
                isHlsStream = true,
                hasIndependentAudio = true,
            ),
        )
    }

    @Test
    fun `keeps video for muxed hls streams without a surface`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isHlsStream = true))
        assertFalse(
            shouldDisableVideoTrack(
                audioOnly = false,
                hasVideoSurface = false,
                isHlsStream = true,
                hasIndependentAudio = false,
            ),
        )
    }

    @Test
    fun `disables video for hls streams with independent audio without a surface`() {
        assertTrue(
            shouldDisableVideoTrack(
                audioOnly = false,
                hasVideoSurface = false,
                isHlsStream = true,
                hasIndependentAudio = true,
            ),
        )
    }

    @Test
    fun `hasIndependentAudioTrack returns false for null or empty tracks`() {
        assertFalse(hasIndependentAudioTrack(null))
        assertFalse(hasIndependentAudioTrack(Tracks.EMPTY))
    }

    @Test
    fun `hasIndependentAudioTrack returns false when only video tracks are present`() {
        val videoGroup = Tracks.Group(
            TrackGroup(
                "main",
                Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )

        assertFalse(hasIndependentAudioTrack(Tracks(listOf(videoGroup))))
    }

    @Test
    fun `hasIndependentAudioTrack returns false when audio is muxed into video`() {
        val videoGroup = Tracks.Group(
            TrackGroup(
                "main",
                Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )
        val muxedAudioGroup = Tracks.Group(
            TrackGroup(
                "main:muxed:0",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .setPrimaryTrackGroupId("main")
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )

        assertFalse(hasIndependentAudioTrack(Tracks(listOf(videoGroup, muxedAudioGroup))))
    }

    @Test
    fun `hasIndependentAudioTrack returns true when an independent audio rendition is supported`() {
        val videoGroup = Tracks.Group(
            TrackGroup(
                "main",
                Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )
        val independentAudioGroup = Tracks.Group(
            TrackGroup(
                "audio:English",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )

        assertTrue(hasIndependentAudioTrack(Tracks(listOf(videoGroup, independentAudioGroup))))
    }

    @Test
    fun `hasIndependentAudioTrack returns true when an independent audio track exceeds capabilities`() {
        val independentAudioGroup = Tracks.Group(
            TrackGroup(
                "audio:English",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_EXCEEDS_CAPABILITIES),
            booleanArrayOf(false),
        )

        assertTrue(hasIndependentAudioTrack(Tracks(listOf(independentAudioGroup))))
    }

    @Test
    fun `hasIndependentAudioTrack returns false when independent audio is unsupported`() {
        val unsupportedAudioGroup = Tracks.Group(
            TrackGroup(
                "audio:English",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AC3)
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_UNSUPPORTED_SUBTYPE),
            booleanArrayOf(false),
        )

        assertFalse(hasIndependentAudioTrack(Tracks(listOf(unsupportedAudioGroup))))
    }

    @Test
    fun `hasIndependentAudioTrack returns true when both muxed and independent audio exist`() {
        val muxedAudioGroup = Tracks.Group(
            TrackGroup(
                "main:muxed:0",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .setPrimaryTrackGroupId("main")
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(false),
        )
        val independentAudioGroup = Tracks.Group(
            TrackGroup(
                "audio:English",
                Format.Builder()
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build(),
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true),
        )

        assertTrue(hasIndependentAudioTrack(Tracks(listOf(muxedAudioGroup, independentAudioGroup))))
    }
}
