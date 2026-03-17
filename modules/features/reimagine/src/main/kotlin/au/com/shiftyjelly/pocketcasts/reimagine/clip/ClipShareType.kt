package au.com.shiftyjelly.pocketcasts.reimagine.clip

import com.automattic.eventhorizon.ShareActionClipType

enum class ClipShareType {
    Audio,
    Video,
    Link,
    ;

    val analyticsValue get() = when (this) {
        Audio -> ShareActionClipType.Audio
        Video -> ShareActionClipType.Video
        Link -> ShareActionClipType.Link
    }
}
