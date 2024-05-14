package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast

interface NovaLauncherManager {
    suspend fun getSubscribedPodcasts(): List<NovaLauncherSubscribedPodcast>
}
