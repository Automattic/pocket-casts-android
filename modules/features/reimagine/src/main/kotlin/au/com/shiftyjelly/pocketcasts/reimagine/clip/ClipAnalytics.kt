package au.com.shiftyjelly.pocketcasts.reimagine.clip

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ShareActionMediaType
import com.automattic.eventhorizon.ShareScreenClipSharedEvent
import com.automattic.eventhorizon.ShareScreenPauseTappedEvent
import com.automattic.eventhorizon.ShareScreenPlayTappedEvent
import com.automattic.eventhorizon.ShareScreenShownEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ClipAnalytics @AssistedInject constructor(
    @Assisted("episodeId") private val episodeId: String,
    @Assisted("podcastId") private val podcastId: String,
    @Assisted("clipId") private val clipId: String,
    @Assisted private val source: SourceView,
    @Assisted private val initialClipRange: Clip.Range,
    private val eventHorizon: EventHorizon,
) {
    fun screenShown() {
        eventHorizon.track(
            ShareScreenShownEvent(
                podcastUuid = podcastId,
                episodeUuid = episodeId,
                clipUuid = clipId,
                source = source.eventHorizonValue,
                type = ShareActionMediaType.Clip,
            ),
        )
    }

    fun playTapped() {
        eventHorizon.track(
            ShareScreenPlayTappedEvent(
                episodeUuid = episodeId,
                podcastUuid = podcastId,
                clipUuid = clipId,
                source = source.eventHorizonValue,
            ),
        )
    }

    fun pauseTapped() {
        eventHorizon.track(
            ShareScreenPauseTappedEvent(
                episodeUuid = episodeId,
                podcastUuid = podcastId,
                clipUuid = clipId,
                source = source.eventHorizonValue,
            ),
        )
    }

    fun clipShared(
        clipRange: Clip.Range,
        shareType: ClipShareType,
        cardType: CardType,
    ) {
        val isStartModified = initialClipRange.startInSeconds != clipRange.startInSeconds
        val isEndModified = initialClipRange.endInSeconds != clipRange.endInSeconds
        eventHorizon.track(
            ShareScreenClipSharedEvent(
                start = clipRange.startInSeconds.toLong(),
                startModified = isStartModified,
                end = clipRange.endInSeconds.toLong(),
                endModified = isEndModified,
                type = shareType.eventHorizonValue,
                cardType = cardType.eventHorizonValue,
                episodeUuid = episodeId,
                podcastUuid = podcastId,
                clipUuid = clipId,
                source = source.eventHorizonValue,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("episodeId") episodeId: String,
            @Assisted("podcastId") podcastId: String,
            @Assisted("clipId") clipId: String,
            sourceView: SourceView,
            initialClipRange: Clip.Range,
        ): ClipAnalytics
    }
}
