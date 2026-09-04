package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.repositories.playback.selectAlternateStream
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlternateEnclosureManagerImpl @Inject constructor(
    private val alternateEnclosureDao: AlternateEnclosureDao,
) : AlternateEnclosureManager {
    override suspend fun findForEpisode(episodeUuid: String): List<EpisodeAlternateEnclosure> = alternateEnclosureDao.findByEpisodeUuid(episodeUuid)

    // A stream-only episode already earns the icon from its own file type, so assume a progressive file here.
    override fun hasVideoAlternateEnclosure(episodeUuid: String): Flow<Boolean> = alternateEnclosureDao.observeByEpisodeUuid(episodeUuid)
        .map { enclosures -> selectAlternateStream(enclosures, hasProgressiveEnclosure = true) != null }
}
