package au.com.shiftyjelly.pocketcasts.models.to

import com.automattic.eventhorizon.ChapterOriginType

fun ChapterOrigin.toChapterOriginType() = when (this) {
    ChapterOrigin.Unknown -> ChapterOriginType.Unknown
    ChapterOrigin.PodcastIndex -> ChapterOriginType.PodcastIndex
    ChapterOrigin.ShowNotes -> ChapterOriginType.ShowNotes
    ChapterOrigin.NativeMedia -> ChapterOriginType.NativeMedia
    ChapterOrigin.Generated -> ChapterOriginType.Generated
}
