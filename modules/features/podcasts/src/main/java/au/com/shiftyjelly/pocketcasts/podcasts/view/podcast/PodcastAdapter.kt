package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeHeaderBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkHeaderViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkUpsellViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.DividerLineViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.NoBookmarkViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.PaddingViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.PodrollViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.RecommendedPodcastViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.TabsViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingTappedSource
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast.RecommendationsResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val differ: DiffUtil.ItemCallback<Any> = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Podcast && newItem is Podcast -> true
            oldItem is PodcastAdapter.TabsHeader && newItem is PodcastAdapter.TabsHeader -> true
            oldItem is PodcastAdapter.EpisodeHeader && newItem is PodcastAdapter.EpisodeHeader -> true
            oldItem is PodcastEpisode && newItem is PodcastEpisode -> oldItem.uuid == newItem.uuid
            oldItem is PodcastAdapter.BookmarkItemData && newItem is PodcastAdapter.BookmarkItemData -> oldItem.bookmark.uuid == newItem.bookmark.uuid
            oldItem is PodcastAdapter.BookmarkHeader && newItem is PodcastAdapter.BookmarkHeader -> true
            oldItem is PodcastAdapter.DividerRow && newItem is PodcastAdapter.DividerRow -> oldItem.groupIndex == newItem.groupIndex
            else -> oldItem == newItem
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is Podcast && newItem is Podcast) {
            return true
        } else if (oldItem is PodcastAdapter.EpisodeHeader && newItem is PodcastAdapter.EpisodeHeader) {
            // don't compare search term because we don't what the row to recreate while typing
            return oldItem.searchTerm.isNotEmpty() || newItem.searchTerm.isNotEmpty() ||
                (
                    oldItem.archivedCount == newItem.archivedCount &&
                        oldItem.episodeCount == newItem.episodeCount &&
                        oldItem.showingArchived == newItem.showingArchived &&
                        oldItem.episodeLimit == newItem.episodeLimit
                    )
        } else if (oldItem is PodcastAdapter.BookmarkHeader && newItem is PodcastAdapter.BookmarkHeader) {
            return oldItem.bookmarksCount == newItem.bookmarksCount
        }
        return oldItem == newItem
    }
}

class PodcastAdapter(
    var fromListUuid: String?,
    private val headerType: HeaderType,
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val theme: Theme,
    private val podcastBookmarksObservable: Observable<List<Bookmark>>,
    private val onHeaderSummaryToggled: (Boolean, Boolean) -> Unit,
    private val onSubscribeClicked: () -> Unit,
    private val onUnsubscribeClicked: (successCallback: () -> Unit) -> Unit,
    private val onEpisodesOptionsClicked: () -> Unit,
    private val onBookmarksOptionsClicked: () -> Unit,
    private val onEpisodeRowLongPress: (PodcastEpisode) -> Unit,
    private val onBookmarkRowLongPress: (Bookmark) -> Unit,
    private val onFoldersClicked: () -> Unit,
    private val onPodcastDescriptionClicked: () -> Unit,
    private val onNotificationsClicked: (Podcast, Boolean) -> Unit,
    private val onDonateClicked: (Uri?) -> Unit,
    private val onSettingsClicked: () -> Unit,
    private val playButtonListener: PlayButton.OnClickListener,
    private val onRowClicked: (PodcastEpisode) -> Unit,
    private val onSearchQueryChanged: (String) -> Unit,
    private val onSearchFocus: () -> Unit,
    private val onShowArchivedClicked: () -> Unit,
    private val multiSelectEpisodesHelper: MultiSelectEpisodesHelper,
    private val multiSelectBookmarksHelper: MultiSelectBookmarksHelper,
    private val onArtworkLongClicked: (successCallback: () -> Unit) -> Unit,
    private val ratingsViewModel: PodcastRatingsViewModel,
    private val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
    private val onTabClicked: (PodcastTab) -> Unit,
    private val onBookmarkPlayClicked: (Bookmark) -> Unit,
    private val onHeadsetSettingsClicked: () -> Unit,
    private val onGetBookmarksClicked: () -> Unit,
    private val onChangeHeaderExpanded: (String, Boolean) -> Unit,
    private val onClickRating: (Podcast, RatingTappedSource) -> Unit,
    private val onClickCategory: (Podcast) -> Unit,
    private val onClickWebsite: (Podcast) -> Unit,
    private val onArtworkAvailable: (Podcast) -> Unit,
    private val onRecommendedPodcastClicked: (String, String) -> Unit,
    private val onRecommendedPodcastSubscribeClicked: (String, String) -> Unit,
    private val onPodrollHeaderClicked: () -> Unit,
    private val onPodrollPodcastClicked: (String) -> Unit,
    private val onPodrollPodcastSubscribeClicked: (String) -> Unit,
) : LargeListAdapter<Any, RecyclerView.ViewHolder>(1500, differ) {

    data class EpisodeLimitRow(val episodeLimit: Int)
    class DividerRow(val grouping: PodcastGrouping, val groupIndex: Int)
    data class NoResultsMessage(val title: String, val bodyText: String, val showButton: Boolean)
    data class EpisodeHeader(val showingArchived: Boolean, val episodeCount: Int, val archivedCount: Int, val searchTerm: String, val episodeLimit: Int?)
    data class TabsHeader(
        val tabs: List<PodcastTab>,
        val selectedTab: PodcastTab,
        val onTabClicked: (PodcastTab) -> Unit,
    )

    data class BookmarkHeader(
        val bookmarksCount: Int,
        val searchTerm: String,
        val onSearchFocus: () -> Unit,
        val onSearchQueryChanged: (String) -> Unit,
        val onOptionsClicked: () -> Unit,
    )

    data class BookmarkItemData(
        val bookmark: Bookmark,
        val episode: BaseEpisode,
        val useEpisodeArtwork: Boolean,
        val onBookmarkPlayClicked: (Bookmark) -> Unit,
        val onBookmarkRowLongPress: (Bookmark) -> Unit,
        val onBookmarkRowClick: (Bookmark, Int) -> Unit,
        val isMultiSelecting: () -> Boolean,
        val isSelected: (Bookmark) -> Boolean,
    )

    object BookmarkUpsell
    object NoBookmarkMessage

    data class PodrollHeaderRow(
        val onClick: () -> Unit,
    )
    object DividerLineRow
    data class PaddingRow(
        val padding: Dp,
    )
    data class RecommendedPodcast(
        val listDate: String,
        val podcast: DiscoverPodcast,
        val onRowClick: (podcastUuid: String, listDate: String) -> Unit,
        val onSubscribeClick: (podcastUuid: String, listDate: String) -> Unit,
    )

    enum class HeaderType {
        Blur,
        Scrim,
    }

    companion object {
        private const val VIEW_TYPE_TABS = 100
        private const val VIEW_TYPE_BOOKMARKS = 101
        private const val VIEW_TYPE_BOOKMARK_HEADER = 102
        private const val VIEW_TYPE_BOOKMARK_UPSELL = 103
        private const val VIEW_TYPE_NO_BOOKMARK = 104
        const val VIEW_TYPE_PODCAST_HEADER = 105
        private const val VIEW_TYPE_RECOMMENDED_PODCAST = 106
        private const val VIEW_TYPE_PODROLL_HEADER = 107
        private const val VIEW_TYPE_DIVIDER_LINE = 108
        private const val VIEW_TYPE_PADDING_ROW = 109
        val VIEW_TYPE_EPISODE_HEADER = R.layout.adapter_episode_header
        val VIEW_TYPE_EPISODE_LIMIT_ROW = R.layout.adapter_episode_limit
        val VIEW_TYPE_NO_RESULTS = R.layout.adapter_no_results
        val VIEW_TYPE_DIVIDER_TITLE = R.layout.adapter_divider_row
    }

    private val disposables = CompositeDisposable()
    private var podcast: Podcast = Podcast()
    private var podcastDescription = AnnotatedString("")

    private var headerExpanded: Boolean = false
    private var isDescriptionExpanded = false
    private var tintColor: Int = 0x000000
    private var signInState: SignInState = SignInState.SignedOut
    private var ratingState: RatingState = RatingState.Loading

    private var bookmarksAvailable: Boolean = false

    var castConnected: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val imageRequestFactory = PocketCastsImageRequestFactory(context).themed()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PODCAST_HEADER -> PodcastHeaderViewHolder(
                context = parent.context,
                theme = theme,
                useBlurredArtwork = headerType == HeaderType.Blur,
                onClickCategory = onClickCategory,
                onClickRating = onClickRating,
                onClickFollow = onSubscribeClicked,
                onClickUnfollow = { onUnsubscribeClicked { } },
                onClickFolder = onFoldersClicked,
                onClickNotification = onNotificationsClicked,
                onClickDonate = onDonateClicked,
                onClickSettings = onSettingsClicked,
                onClickWebsiteLink = onClickWebsite,
                onToggleHeader = {
                    onChangeHeaderExpanded(podcast.uuid, !podcast.isHeaderExpanded)
                },
                onToggleDescription = {
                    isDescriptionExpanded = !isDescriptionExpanded
                    notifyItemChanged(0)
                },
                onLongClickArtwork = {
                    onArtworkLongClicked { notifyItemChanged(0) }
                },
                onArtworkAvailable = onArtworkAvailable,
            )

            VIEW_TYPE_TABS -> TabsViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_EPISODE_HEADER -> EpisodeHeaderViewHolder(AdapterEpisodeHeaderBinding.inflate(inflater, parent, false), onEpisodesOptionsClicked, onSearchFocus)
            VIEW_TYPE_EPISODE_LIMIT_ROW -> EpisodeLimitViewHolder(inflater.inflate(R.layout.adapter_episode_limit, parent, false))
            VIEW_TYPE_NO_RESULTS -> NoResultsViewHolder(inflater.inflate(R.layout.adapter_no_results, parent, false))
            VIEW_TYPE_DIVIDER_TITLE -> DividerTitleViewHolder(inflater.inflate(R.layout.adapter_divider_row, parent, false))
            VIEW_TYPE_BOOKMARKS -> BookmarkViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_HEADER -> BookmarkHeaderViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_UPSELL -> BookmarkUpsellViewHolder(ComposeView(parent.context), onGetBookmarksClicked, theme)
            VIEW_TYPE_NO_BOOKMARK -> NoBookmarkViewHolder(ComposeView(parent.context), theme, onHeadsetSettingsClicked)
            VIEW_TYPE_RECOMMENDED_PODCAST -> RecommendedPodcastViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_PODROLL_HEADER -> PodrollViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_DIVIDER_LINE -> DividerLineViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_PADDING_ROW -> PaddingViewHolder(ComposeView(parent.context))
            else -> EpisodeViewHolder(
                binding = AdapterEpisodeBinding.inflate(inflater, parent, false),
                viewMode = if (settings.artworkConfiguration.value.useEpisodeArtwork(Element.Podcasts)) {
                    EpisodeViewHolder.ViewMode.Artwork
                } else {
                    EpisodeViewHolder.ViewMode.NoArtwork
                },
                downloadProgressUpdates = downloadManager.progressUpdateRelay,
                playbackStateUpdates = playbackManager.playbackStateRelay,
                upNextChangesObservable = upNextQueue.changesObservable,
                imageRequestFactory = imageRequestFactory.smallSize(),
                settings = settings,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory,
                artworkContext = Element.Podcasts,
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PodcastHeaderViewHolder -> holder.bind(
                podcast,
                podcastDescription,
                isDescriptionExpanded,
                ratingState,
                signInState,
            )

            is EpisodeViewHolder -> bindEpisodeViewHolder(holder, position, fromListUuid)
            is TabsViewHolder -> holder.bind(getItem(position) as TabsHeader)
            is EpisodeHeaderViewHolder -> bindingEpisodeHeaderViewHolder(holder, position)
            is EpisodeLimitViewHolder -> bindEpisodeLimitRow(holder, position)
            is NoResultsViewHolder -> bindNoResultsMessage(holder, position)
            is DividerTitleViewHolder -> bindDividerRow(holder, position)
            is BookmarkViewHolder -> holder.bind(getItem(position) as BookmarkItemData)
            is BookmarkHeaderViewHolder -> holder.bind(getItem(position) as BookmarkHeader)
            is BookmarkUpsellViewHolder -> holder.bind()
            is NoBookmarkViewHolder -> holder.bind()
            is RecommendedPodcastViewHolder -> holder.bind(getItem(position) as RecommendedPodcast)
            is PodrollViewHolder -> holder.bind(getItem(position) as PodrollHeaderRow)
            is DividerLineViewHolder -> holder.bind()
            is PaddingViewHolder -> holder.bind(getItem(position) as PaddingRow)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

    private fun bindingEpisodeHeaderViewHolder(holder: EpisodeHeaderViewHolder, position: Int) {
        val episodeHeader = getItem(position) as? EpisodeHeader ?: return
        holder.binding.episodesSummary.let {
            val quantityString = if (episodeHeader.episodeCount == 1) {
                it.context.resources.getString(LR.string.podcast_episode_summary_singular, episodeHeader.archivedCount)
            } else {
                it.context.resources.getString(LR.string.podcast_episode_summary_plural, episodeHeader.episodeCount, episodeHeader.archivedCount)
            }
            val text = if (episodeHeader.episodeLimit != null) {
                val limited = "Limited to ${episodeHeader.episodeLimit}"
                val spannable = SpannableString("$quantityString • $limited")
                val color = ContextCompat.getColor(holder.itemView.context, UR.color.orange_50)
                spannable.setSpan(ForegroundColorSpan(color), spannable.length - limited.length, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                spannable
            } else {
                SpannableString(quantityString)
            }
            it.text = text
        }
        holder.binding.episodeSearchView.apply {
            onSearch = { query ->
                onSearchQueryChanged(query)
            }
            text = episodeHeader.searchTerm
        }
        holder.binding.btnArchived.setText(if (episodeHeader.showingArchived) LR.string.podcast_hide_archived else LR.string.podcast_show_archived)
        holder.binding.btnArchived.setOnClickListener { onShowArchivedClicked() }
    }

    private fun bindEpisodeViewHolder(holder: EpisodeViewHolder, position: Int, fromListUuid: String?) {
        val episode = getItem(position) as? PodcastEpisode ?: return
        holder.setup(
            episode = episode,
            fromListUuid = fromListUuid,
            tintColor = ThemeColor.podcastIcon02(theme.activeTheme, tintColor),
            playButtonListener = playButtonListener,
            streamByDefault = settings.streamingMode.value || castConnected,
            upNextAction = settings.upNextSwipe.value,
            multiSelectEnabled = multiSelectEpisodesHelper.isMultiSelecting,
            isSelected = multiSelectEpisodesHelper.isSelected(episode),
            disposables = disposables,
            bookmarksObservable = podcastBookmarksObservable,
            bookmarksAvailable = bookmarksAvailable,
        )
        holder.episodeRow.setOnClickListener {
            if (multiSelectEpisodesHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectEpisodesHelper.toggle(episode)
            } else {
                onRowClicked(episode)
            }
        }
        holder.episodeRow.setOnLongClickListener {
            onEpisodeRowLongPress(episode)
            true
        }
    }

    private fun bindEpisodeLimitRow(holder: EpisodeLimitViewHolder, position: Int) {
        val episodeLimitRow = getItem(position) as? EpisodeLimitRow ?: return
        val limit = episodeLimitRow.episodeLimit
        holder.lblTitle.text = holder.itemView.resources.getString(LR.string.podcast_episodes_limited, limit)
    }

    private fun bindNoResultsMessage(holder: NoResultsViewHolder, position: Int) {
        val noResultsMessage = getItem(position) as? NoResultsMessage ?: return
        holder.lblTitle.text = noResultsMessage.title
        holder.lblBody.text = noResultsMessage.bodyText
        holder.btnShowArchived.setOnClickListener { onShowArchivedClicked() }
        holder.btnShowArchived.isVisible = noResultsMessage.showButton
    }

    private fun bindDividerRow(holder: DividerTitleViewHolder, position: Int) {
        val dividerRow = getItem(position) as? DividerRow ?: return
        val title = dividerRow.grouping.groupTitles(dividerRow.groupIndex, holder.lblTitle.context)
        if (title.isNotEmpty()) {
            holder.lblTitle.visibility = View.VISIBLE
            holder.lblTitle.text = title
        } else {
            holder.lblTitle.visibility = View.GONE
        }
    }

    fun setPodcast(podcast: Podcast) {
        // expand the podcast description and details if the user hasn't subscribed
        if (this.podcast.uuid != podcast.uuid) {
            headerExpanded = !podcast.isSubscribed
            ratingsViewModel.loadRatings(podcast.uuid)
            ratingsViewModel.refreshPodcastRatings(podcast.uuid)
            onHeaderSummaryToggled(headerExpanded, false)
        }
        this.podcast = podcast
        val isHtmlDescription = FeatureFlag.isEnabled(Feature.PODCAST_HTML_DESCRIPTION) && podcast.podcastHtmlDescription.isNotEmpty()
        val rawDescription = if (isHtmlDescription) { podcast.podcastHtmlDescription } else { podcast.podcastDescription }
        this.podcastDescription = HtmlCompat.fromHtml(
            rawDescription,
            HtmlCompat.FROM_HTML_MODE_COMPACT and
                HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH.inv(), // keep the extra line break from paragraphs as it looks better
        ).toAnnotatedString(urlColor = ThemeColor.podcastText02(theme.activeTheme, tintColor))

        notifyDataSetChanged()
    }

    fun setTint(tintColor: Int) {
        this.tintColor = tintColor
    }

    fun setSignInState(signInState: SignInState) {
        this.signInState = signInState
        notifyItemChanged(0)
    }

    fun setRatingState(state: RatingState) {
        this.ratingState = state
        notifyItemChanged(0)
    }

    fun setEpisodes(
        episodes: List<PodcastEpisode>,
        showingArchived: Boolean,
        episodeCount: Int,
        archivedCount: Int,
        searchTerm: String,
        episodeLimit: Int?,
        episodeLimitIndex: Int?,
        podcast: Podcast,
        tabs: List<PodcastTab>,
        context: Context,
    ) {
        val grouping = podcast.grouping
        val groupingFunction = grouping.sortFunction
        val episodesPlusLimit: MutableList<Any> = episodes.toMutableList()
        if (episodeLimit != null && episodeLimitIndex != null && groupingFunction == null) {
            if (searchTerm.isEmpty() && (podcast.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_ASC || podcast.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_DESC)) {
                episodesPlusLimit.add(episodeLimitIndex, EpisodeLimitRow(episodeLimit))
            }
        }
        if (groupingFunction != null) {
            val grouped = grouping.formGroups(episodes, podcast, context.resources)
            episodesPlusLimit.clear()

            grouped.forEachIndexed { index, list ->
                if (list.isNotEmpty()) {
                    episodesPlusLimit.add(DividerRow(grouping, index))
                    episodesPlusLimit.addAll(list)
                }
            }
        }
        val content = mutableListOf<Any>().apply {
            add(Podcast())
            add(TabsHeader(tabs = tabs, selectedTab = PodcastTab.EPISODES, onTabClicked = onTabClicked))
            add(
                EpisodeHeader(
                    showingArchived = showingArchived,
                    episodeCount = episodeCount,
                    archivedCount = archivedCount,
                    searchTerm = searchTerm,
                    episodeLimit = podcast.autoArchiveEpisodeLimit?.value,
                ),
            )
            addAll(episodesPlusLimit)
        }

        if (episodes.isEmpty()) {
            if (searchTerm.isEmpty()) {
                if (archivedCount == 0) {
                    content.add(
                        NoResultsMessage(
                            title = context.getString(LR.string.podcast_no_episodes_found),
                            bodyText = context.getString(LR.string.podcast_no_episodes),
                            showButton = false,
                        ),
                    )
                } else {
                    content.add(
                        NoResultsMessage(
                            title = context.getString(LR.string.podcast_no_episodes_found),
                            bodyText = context.getString(LR.string.podcast_no_episodes_all_archived, archivedCount),
                            showButton = true,
                        ),
                    )
                }
            } else {
                content.add(
                    NoResultsMessage(
                        title = context.getString(LR.string.podcast_no_episodes_found),
                        bodyText = context.getString(LR.string.podcast_no_episodes_matching),
                        showButton = false,
                    ),
                )
            }
        }

        submitList(content)
    }

    fun setBookmarks(
        bookmarks: List<Bookmark>,
        episodes: List<BaseEpisode>,
        searchTerm: String,
        tabs: List<PodcastTab>,
        context: Context,
    ) {
        val content = mutableListOf<Any>().apply {
            add(Podcast())
            add(TabsHeader(tabs = tabs, selectedTab = PodcastTab.BOOKMARKS, onTabClicked = onTabClicked))

            if (!bookmarksAvailable) {
                add(BookmarkUpsell)
            } else if (searchTerm.isEmpty() && bookmarks.isEmpty()) {
                add(NoBookmarkMessage)
            } else {
                add(
                    BookmarkHeader(
                        bookmarksCount = bookmarks.size,
                        searchTerm = searchTerm,
                        onSearchFocus = onSearchFocus,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onOptionsClicked = onBookmarksOptionsClicked,
                    ),
                )
                if (searchTerm.isNotEmpty() && bookmarks.isEmpty()) {
                    add(
                        NoResultsMessage(
                            title = context.getString(LR.string.podcast_no_bookmarks_found),
                            bodyText = context.getString(LR.string.podcast_no_bookmarks_matching),
                            showButton = false,
                        ),
                    )
                } else {
                    addAll(
                        bookmarks.map {
                            BookmarkItemData(
                                bookmark = it,
                                episode = episodes.find { episode -> episode.uuid == it.episodeUuid } ?: NoOpEpisode,
                                onBookmarkPlayClicked = onBookmarkPlayClicked,
                                onBookmarkRowLongPress = onBookmarkRowLongPress,
                                onBookmarkRowClick = { bookmark, adapterPosition ->
                                    multiSelectBookmarksHelper.toggle(bookmark)
                                    notifyItemChanged(adapterPosition)
                                },
                                isMultiSelecting = { multiSelectBookmarksHelper.isMultiSelecting },
                                isSelected = { bookmark ->
                                    multiSelectBookmarksHelper.isSelected(
                                        bookmark,
                                    )
                                },
                                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.Bookmarks),
                            )
                        },
                    )
                }
            }
        }
        submitList(content)
    }

    fun setRecommendations(
        result: RecommendationsResult,
        tabs: List<PodcastTab>,
    ) {
        val content = buildList {
            add(Podcast())
            add(TabsHeader(tabs = tabs, selectedTab = PodcastTab.RECOMMENDATIONS, onTabClicked = onTabClicked))
            if (result is RecommendationsResult.Success) {
                val list = result.listFeed
                // Podroll
                val podroll = list.podroll
                if (!podroll.isNullOrEmpty()) {
                    add(PodrollHeaderRow(onClick = onPodrollHeaderClicked))
                    podroll.forEachIndexed { index, podcast ->
                        add(
                            RecommendedPodcast(
                                listDate = list.date ?: "",
                                podcast = podcast,
                                onRowClick = { podcastUuid, _ -> onPodrollPodcastClicked(podcastUuid) },
                                onSubscribeClick = { podcastUuid, _ -> onPodrollPodcastSubscribeClicked(podcastUuid) },
                            ),
                        )
                    }
                    add(PaddingRow(12.dp))
                    add(DividerLineRow)
                }
                add(PaddingRow(12.dp))
                // Recommended podcasts
                val podcasts = list.podcasts
                podcasts?.forEachIndexed { index, podcast ->
                    add(
                        RecommendedPodcast(
                            listDate = list.date ?: "",
                            podcast = podcast,
                            onRowClick = onRecommendedPodcastClicked,
                            onSubscribeClick = onRecommendedPodcastSubscribeClicked,
                        ),
                    )
                }
                add(PaddingRow(12.dp))
            }
        }
        submitList(content)
    }

    fun setBookmarksAvailable(bookmarksAvailable: Boolean) {
        this.bookmarksAvailable = bookmarksAvailable
    }

    fun setError() {
        submitList(emptyList())
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is Podcast -> VIEW_TYPE_PODCAST_HEADER
            is EpisodeHeader -> R.layout.adapter_episode_header
            is EpisodeLimitRow -> R.layout.adapter_episode_limit
            is NoResultsMessage -> R.layout.adapter_no_results
            is DividerRow -> R.layout.adapter_divider_row
            is TabsHeader -> VIEW_TYPE_TABS
            is BookmarkItemData -> VIEW_TYPE_BOOKMARKS
            is BookmarkHeader -> VIEW_TYPE_BOOKMARK_HEADER
            is BookmarkUpsell -> VIEW_TYPE_BOOKMARK_UPSELL
            is NoBookmarkMessage -> VIEW_TYPE_NO_BOOKMARK
            is RecommendedPodcast -> VIEW_TYPE_RECOMMENDED_PODCAST
            is PodrollHeaderRow -> VIEW_TYPE_PODROLL_HEADER
            is DividerLineRow -> VIEW_TYPE_DIVIDER_LINE
            is PaddingRow -> VIEW_TYPE_PADDING_ROW
            else -> R.layout.adapter_episode
        }
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return when (item) {
            is Podcast -> Long.MAX_VALUE
            is EpisodeHeader -> Long.MAX_VALUE - 1
            is EpisodeLimitRow -> Long.MAX_VALUE - 2
            is NoResultsMessage -> Long.MAX_VALUE - 3
            is TabsHeader -> Long.MAX_VALUE - 4
            is BookmarkHeader -> Long.MAX_VALUE - 5
            is BookmarkUpsell -> Long.MAX_VALUE - 6
            is NoBookmarkMessage -> Long.MAX_VALUE - 7
            is DividerRow -> item.groupIndex.toLong()
            is PodcastEpisode -> item.adapterId
            is BookmarkItemData -> item.bookmark.adapterId
            is RecommendedPodcast -> item.podcast.adapterId
            is PodrollHeaderRow -> Long.MAX_VALUE - 8
            is DividerLineRow -> Long.MAX_VALUE - 9
            is PaddingRow -> Long.MAX_VALUE - 10
            else -> throw IllegalStateException("Unknown item type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EpisodeViewHolder) {
            holder.clearObservers()
        }
    }

    internal class EpisodeHeaderViewHolder(val binding: AdapterEpisodeHeaderBinding, val onEpisodesOptionsClicked: () -> Unit, val onSearchFocus: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnEpisodeOptions.setOnClickListener {
                onEpisodesOptionsClicked()
            }
            binding.episodeSearchView.onFocus = {
                onSearchFocus()
            }
        }
    }

    internal class EpisodeLimitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)
    }

    internal class NoResultsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)
        val lblBody = itemView.findViewById<TextView>(R.id.lblBody)
        val btnShowArchived = itemView.findViewById<View>(R.id.btnShowArchived)
    }

    internal class DividerTitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)
    }

    private inner class PodcastHeaderViewHolder(
        context: Context,
        private val theme: Theme,
        private val useBlurredArtwork: Boolean,
        private val onClickCategory: (Podcast) -> Unit,
        private val onClickRating: (Podcast, RatingTappedSource) -> Unit,
        private val onClickFollow: () -> Unit,
        private val onClickUnfollow: () -> Unit,
        private val onClickFolder: () -> Unit,
        private val onClickNotification: (Podcast, Boolean) -> Unit,
        private val onClickDonate: (Uri?) -> Unit,
        private val onClickSettings: () -> Unit,
        private val onClickWebsiteLink: (Podcast) -> Unit,
        private val onToggleHeader: () -> Unit,
        private val onToggleDescription: () -> Unit,
        private val onLongClickArtwork: () -> Unit,
        private val onArtworkAvailable: (Podcast) -> Unit,
    ) : RecyclerView.ViewHolder(ComposeView(context)) {
        private val composeView get() = itemView as ComposeView

        init {
            composeView.setTag(UR.id.podcast_view_header_tag, true)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        fun bind(
            podcast: Podcast,
            podcastDescription: AnnotatedString,
            isDescriptionExpanded: Boolean,
            ratingState: RatingState,
            signInState: SignInState,
        ) {
            composeView.setContent {
                // See cachedStatusBarPadding for explanation.
                val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding()
                    .coerceAtLeast(cachedStatusBarPadding)
                cachedStatusBarPadding = statusBarPadding

                AppTheme(theme.activeTheme) {
                    PodcastHeader(
                        uuid = podcast.uuid,
                        title = podcast.title,
                        category = podcast.getFirstCategory(itemView.context.resources),
                        author = podcast.author,
                        description = podcastDescription,
                        podcastInfoState = PodcastInfoState(
                            author = podcast.author,
                            link = podcast.getShortUrl(),
                            schedule = podcast.displayableFrequency(context.resources),
                            next = podcast.displayableNextEpisodeDate(context),
                        ),
                        rating = ratingState,
                        isFollowed = podcast.isSubscribed,
                        areNotificationsEnabled = podcast.isShowNotifications,
                        isFundingUrlAvailable = podcast.fundingUrl != null,
                        folderIcon = when {
                            !signInState.isSignedInAsPlusOrPatron -> PodcastFolderIcon.BuyFolders
                            podcast.folderUuid != null -> PodcastFolderIcon.AddedToFolder
                            else -> PodcastFolderIcon.NotInFolder
                        },
                        isHeaderExpanded = podcast.isHeaderExpanded,
                        isDescriptionExpanded = isDescriptionExpanded,
                        contentPadding = PaddingValues(
                            top = statusBarPadding + 40.dp, // Eyeball the position inside app bar
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                        useBlurredArtwork = useBlurredArtwork,
                        onClickCategory = { onClickCategory(podcast) },
                        onClickRating = { source -> onClickRating(podcast, source) },
                        onClickFollow = onClickFollow,
                        onClickUnfollow = onClickUnfollow,
                        onClickFolder = onClickFolder,
                        onClickNotification = { onClickNotification(podcast, !podcast.isShowNotifications) },
                        onClickDonate = { onClickDonate(podcast.fundingUrl?.toUri()) },
                        onClickSettings = onClickSettings,
                        onClickWebsiteLink = { onClickWebsiteLink(podcast) },
                        onToggleHeader = onToggleHeader,
                        onToggleDescription = onToggleDescription,
                        onLongClickArtwork = onLongClickArtwork,
                        onArtworkAvailable = { onArtworkAvailable(podcast) },
                    )
                }
            }
        }
    }

    private val NoOpEpisode = PodcastEpisode(uuid = "", publishedDate = Date())
}

// We can't simply apply 'WindowInsets.statusBars' inset.
// When navigating to this screen the inset isn't available before first layout
// which can cause an ugly jump effect.
//
// I'm not exactly sure why it happens only in this scenario but I assume it has
// something to do with mix of recycler view and compose.
//
// 48.dp is a standard status bar height and should be good enough for the initial pass.
private var cachedStatusBarPadding: Dp = 48.dp
