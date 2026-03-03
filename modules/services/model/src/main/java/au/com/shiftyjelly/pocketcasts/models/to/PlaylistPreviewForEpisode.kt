package au.com.shiftyjelly.pocketcasts.models.to

data class PlaylistPreviewForEpisode(
    val uuid: String,
    val title: String,
    val episodeUuids: Set<String>,
) {
    fun hasAllEpisodes(uuids: Collection<String>): Boolean {
        return uuids.all { uuid -> uuid in episodeUuids }
    }

    fun canAddOrRemoveEpisodes(episodeLimit: Int, newEpisodes: Collection<String>): Boolean {
        return hasAllEpisodes(newEpisodes) || (episodeUuids + newEpisodes).size <= episodeLimit
    }
}
