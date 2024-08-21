package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "curated_podcasts",
    primaryKeys = ["list_id", "podcast_id"],
    indices = [
        Index(value = ["list_id"], name = "curated_podcasts_list_id_index", unique = false),
        Index(value = ["podcast_id"], name = "curated_podcasts_podcast_id_index", unique = false),
    ],
)
data class CuratedPodcast(
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "list_title") val listTitle: String,
    @ColumnInfo(name = "podcast_id") val podcastId: String,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String,
    @ColumnInfo(name = "podcast_description") val podcastDescription: String?,
) {
    companion object {
        const val TRENDING_LIST_ID = "trending"
        const val FEATURED_LIST_ID = "featured"

        val specialListIds = listOf(TRENDING_LIST_ID, FEATURED_LIST_ID)
    }
}
