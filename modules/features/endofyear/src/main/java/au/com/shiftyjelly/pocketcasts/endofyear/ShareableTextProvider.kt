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
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryYearOverYear
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManager
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class ShareableTextProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listServerManager: ListServerManager,
) {
    var chosenActivity: String? = null
    private var shortURL: String = Settings.SERVER_SHORT_URL
    private val hashtags = listOf("pocketcasts", "playback2023").joinToString(" ") { "#$it" }

    suspend fun getShareableDataForStory(
        story: Story,
    ): ShareTextData {
        var showShortURLAtEnd = false
        val shareableLink: String = when (story) {
            is StoryTopFivePodcasts -> {
                try {
                    listServerManager.createPodcastList(
                        title = context.resources.getString(
                            LR.string.end_of_year_story_top_podcasts_share_text,
                            ""
                        ),
                        description = "",
                        podcasts = story.topPodcasts.map { it.toPodcast() }
                    ) ?: shortURL
                } catch (ex: Exception) {
                    Timber.e(ex)
                    shortURL
                }
            }
            is StoryTopPodcast -> {
                "$shortURL/podcast/${story.topPodcast.uuid}"
            }
            is StoryLongestEpisode -> {
                "$shortURL/episode/${story.longestEpisode.uuid}"
            }
            else -> {
                showShortURLAtEnd = true
                shortURL
            }
        }
        val textWithLink = getTextWithLink(story, shareableLink)
        return ShareTextData(
            textWithLink = textWithLink,
            hashTags = hashtags,
            showShortURLAtEnd = showShortURLAtEnd
        )
    }

    private fun getTextWithLink(
        story: Story,
        shareableLink: String
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
                    shareableLink
                )
            }

            is StoryTopFivePodcasts -> {
                resources.getString(
                    LR.string.end_of_year_story_top_podcasts_share_text,
                    shareableLink
                )
            }

            is StoryLongestEpisode -> {
                resources.getString(
                    LR.string.end_of_year_story_longest_episode_share_text,
                    shareableLink
                )
            }

            is StoryYearOverYear -> {
                resources.getString(
                    LR.string.end_of_year_stories_year_over_share_text
                )
            }

            else -> ""
        }
    }

    data class ShareTextData(
        val textWithLink: String,
        val hashTags: String,
        val showShortURLAtEnd: Boolean
    )
}
