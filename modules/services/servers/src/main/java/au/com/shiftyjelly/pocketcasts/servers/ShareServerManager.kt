package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

interface ShareServerManager {

    fun sharePodcastList(title: String, description: String, podcasts: List<Podcast>, callback: SendPodcastListCallback)
    fun loadPodcastList(id: String, callback: PodcastListCallback)
    fun extractShareListIdFromWebUrl(webUrl: String?): String?

    interface SendPodcastListCallback {
        fun onSuccess(url: String)
        fun onFailed()
    }

    interface PodcastListCallback {
        fun onSuccess(title: String?, description: String?, podcasts: List<Podcast>?)
        fun onFailed()
    }
}
