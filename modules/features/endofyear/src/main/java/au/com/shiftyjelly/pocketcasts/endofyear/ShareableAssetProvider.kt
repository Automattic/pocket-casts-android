package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManager
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ShareableAssetProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listServerManager: ListServerManager,
) {
    private var shortURL: String = Settings.SERVER_SHORT_URL
    private val hashtags = listOf("pocketcasts", "endofyear2022").joinToString(" ") { "#$it" }

    suspend fun getShareableDataForStory(
        story: Story,
    ): ShareTextData {
        val text = getText(story)
        val shareableLink: String = when (story) {
            is StoryTopFivePodcasts -> {
                listServerManager.createPodcastList(
                    title = getText(story),
                    description = "",
                    podcasts = story.topPodcasts.map { it.toPodcast() }
                ) ?: shortURL
            }
            is StoryTopPodcast -> {
                "$shortURL/podcast/${story.topPodcast.uuid}"
            }
            is StoryLongestEpisode -> {
                "$shortURL/episode/${story.longestEpisode.uuid}"
            }
            else -> shortURL
        }
        return ShareTextData(
            text = text,
            link = shareableLink,
            hashTags = hashtags
        )
    }

    private fun getText(
        story: Story,
    ): String {
        val resources = context.resources
        return when (story) {
            is StoryListeningTime -> {
                val timeText =
                    StatsHelper.secondsToFriendlyString(story.listeningTimeInSecs, resources)
                resources.getString(
                    LR.string.end_of_year_story_listened_to_share_text,
                    timeText
                )
            }

            is StoryListenedCategories -> {
                resources.getString(
                    LR.string.end_of_year_story_listened_to_categories_share_text,
                    story.listenedCategories.size
                )
            }

            is StoryListenedNumbers -> {
                resources.getString(
                    LR.string.end_of_year_story_listened_to_numbers_share_text,
                    story.listenedNumbers.numberOfPodcasts,
                    story.listenedNumbers.numberOfEpisodes
                )
            }

            is StoryTopPodcast -> {
                resources.getString(
                    LR.string.end_of_year_story_top_podcast_share_text,
                    story.topPodcast.title
                )
            }

            is StoryTopFivePodcasts -> {
                resources.getString(
                    LR.string.end_of_year_story_top_podcasts_share_text,
                    story.topPodcasts[0].title
                )
            }

            is StoryLongestEpisode -> {
                resources.getString(
                    LR.string.end_of_year_story_longest_episode_share_text,
                    story.longestEpisode.title
                )
            }

            else -> ""
        }
    }

    data class ShareTextData(
        val text: String,
        val hashTags: String,
        val link: String,
    )
}
