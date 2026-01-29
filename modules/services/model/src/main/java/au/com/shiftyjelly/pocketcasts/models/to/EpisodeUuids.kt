package au.com.shiftyjelly.pocketcasts.models.to

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EpisodeUuids(
    val episodeUuid: String,
    val podcastUuid: String,
) : Parcelable
