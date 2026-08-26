package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AlternateEnclosureManagerImpl @Inject constructor(
    private val alternateEnclosureDao: AlternateEnclosureDao,
) : AlternateEnclosureManager {
    override suspend fun findForEpisode(episodeUuid: String): List<EpisodeAlternateEnclosure> = alternateEnclosureDao.findByEpisodeUuid(episodeUuid)

    override fun hasHlsAlternateEnclosure(episodeUuid: String): Flow<Boolean> = alternateEnclosureDao.hasHlsEnclosure(episodeUuid, BaseEpisode.HLS_MIME_TYPES)
}
