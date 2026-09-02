package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AlternateEnclosureManagerImpl @Inject constructor(
    private val alternateEnclosureDao: AlternateEnclosureDao,
) : AlternateEnclosureManager {
    override suspend fun findForEpisode(episodeUuid: String): List<EpisodeAlternateEnclosure> = alternateEnclosureDao.findByEpisodeUuid(episodeUuid)

    override fun hasVideoAlternateEnclosure(episodeUuid: String): Flow<Boolean> = alternateEnclosureDao.hasVideoEnclosure(
        episodeUuid = episodeUuid,
        hlsMimeTypes = BaseEpisode.HLS_MIME_TYPES,
        videoMediaKind = MediaKind.Video.stringValue,
    )
}
