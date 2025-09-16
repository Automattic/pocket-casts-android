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
import androidx.annotation.DrawableRes
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeHeaderBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkHeaderViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkUpsellViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.DividerSubTitleViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.EmptyListViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.LoadingViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.PaddingViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.RecommendedPodcastViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.TabsViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingTappedSource
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast.RecommendationsResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import io.reactivex.disposables.CompositeDisposable
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
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
            return oldItem.searchTerm.isNotEmpty() ||
                newItem.searchTerm.isNotEmpty() ||
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
    private val rowDataProvider: EpisodeRowDataProvider,
    private val settings: Settings,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val theme: Theme,
    private val onHeaderSummaryToggled: (Boolean, Boolean) -> Unit,
    private val onSubscribeClicked: () -> Unit,
    private val onUnsubscribeClicked: (successCallback: () -> Unit) -> Unit,
    private val onEpisodesOptionsClicked: () -> Unit,
    private val onBookmarksOptionsClicked: () -> Unit,
    private val onEpisodeRowLongPress: (PodcastEpisode) -> Unit,
    private val onBookmarkRowLongPress: (Bookmark) -> Unit,
    private val onFoldersClicked: () -> Unit,
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
    private val onTabClicked: (PodcastTab) -> Unit,
    private val onBookmarkPlayClicked: (Bookmark) -> Unit,
    private val onHeadsetSettingsClicked: () -> Unit,
    private val onGetBookmarksClicked: () -> Unit,
    private val onChangeHeaderExpanded: (String, Boolean) -> Unit,
    private val onClickRating: (Podcast, RatingTappedSource) -> Unit,
    private val onClickCategory: (Podcast) -> Unit,
    private val onClickWebsite: (Podcast) -> Unit,
    private val onArtworkAvailable: (Podcast) -> Unit,
    private val onRecommendedRetryClicked: () -> Unit,
    private val onRecommendedPodcastClicked: (String, String) -> Unit,
    private val onRecommendedPodcastSubscribeClicked: (String, String) -> Unit,
    private val onPodrollHeaderClicked: () -> Unit,
    private val onPodrollPodcastClicked: (String) -> Unit,
    private val onPodrollPodcastSubscribeClicked: (String) -> Unit,
    private val onSwipeAction: (PodcastEpisode, SwipeAction) -> Unit,
) : LargeListAdapter<Any, RecyclerView.ViewHolder>(1500, differ) {

    data class EpisodeLimitRow(val episodeLimit: Int)
    class DividerRow(val grouping: PodcastGrouping, val groupIndex: Int)
    data class EpisodeHeader(val showingArchived: Boolean, val episodeCount: Int, val archivedCount: Int, val searchTerm: String, val episodeLimit: Int?)
    data class TabsHeader(
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

    data class EmptyList(
        val title: String,
        val subtitle: String = "",
        val iconResourceId: Int,
        val buttonText: String? = null,
        val onButtonClick: (() -> Unit)? = null,
    )

    data class DividerSubTitleRow(
        @DrawableRes val icon: Int,
        val title: String,
        val onClick: (() -> Unit)? = null,
    )

    data class PaddingRow(
        val padding: Dp,
    )

    object LoadingRow

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
        private const val VIEW_TYPE_EMPTY_LIST = 104
        const val VIEW_TYPE_PODCAST_HEADER = 105
        private const val VIEW_TYPE_RECOMMENDED_PODCAST = 106
        private const val VIEW_TYPE_DIVIDER_SUBTITLE = 107
        private const val VIEW_TYPE_PADDING_ROW = 108
        private const val VIEW_TYPE_LOADING_ROW = 109
        val VIEW_TYPE_EPISODE_HEADER = R.layout.adapter_episode_header
        val VIEW_TYPE_EPISODE_LIMIT_ROW = R.layout.adapter_episode_limit
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
            VIEW_TYPE_DIVIDER_TITLE -> DividerTitleViewHolder(inflater.inflate(R.layout.adapter_divider_row, parent, false))
            VIEW_TYPE_BOOKMARKS -> BookmarkViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_HEADER -> BookmarkHeaderViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_UPSELL -> BookmarkUpsellViewHolder(ComposeView(parent.context), onGetBookmarksClicked, theme)
            VIEW_TYPE_EMPTY_LIST -> EmptyListViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_RECOMMENDED_PODCAST -> RecommendedPodcastViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_DIVIDER_SUBTITLE -> DividerSubTitleViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_PADDING_ROW -> PaddingViewHolder(ComposeView(parent.context))
            VIEW_TYPE_LOADING_ROW -> LoadingViewHolder(ComposeView(parent.context), theme)
            else -> {
                val binding = AdapterEpisodeBinding.inflate(inflater, parent, false)
                EpisodeViewHolder(
                    binding = binding,
                    showArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.Podcasts),
                    fromListUuid = null,
                    imageRequestFactory = imageRequestFactory,
                    swipeRowActionsFactory = swipeRowActionsFactory,
                    rowDataProvider = rowDataProvider,
                    playButtonListener = playButtonListener,
                    onRowClick = { episode ->
                        if (multiSelectEpisodesHelper.isMultiSelecting) {
                            binding.checkbox.isChecked = multiSelectEpisodesHelper.toggle(episode)
                        } else {
                            onRowClicked(episode)
                        }
                    },
                    onRowLongClick = onEpisodeRowLongPress,
                    onSwipeAction = onSwipeAction,
                )
            }
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

            is EpisodeViewHolder -> bindEpisodeViewHolder(holder, position, animateMultiSelection = false)
            is TabsViewHolder -> holder.bind(getItem(position) as TabsHeader)
            is EpisodeHeaderViewHolder -> bindingEpisodeHeaderViewHolder(holder, position)
            is EpisodeLimitViewHolder -> bindEpisodeLimitRow(holder, position)
            is DividerTitleViewHolder -> bindDividerRow(holder, position)
            is BookmarkViewHolder -> holder.bind(getItem(position) as BookmarkItemData)
            is BookmarkHeaderViewHolder -> holder.bind(getItem(position) as BookmarkHeader)
            is BookmarkUpsellViewHolder -> holder.bind()
            is EmptyListViewHolder -> holder.bind(getItem(position) as EmptyList)
            is RecommendedPodcastViewHolder -> holder.bind(getItem(position) as RecommendedPodcast)
            is DividerSubTitleViewHolder -> holder.bind(getItem(position) as DividerSubTitleRow)
            is PaddingViewHolder -> holder.bind(getItem(position) as PaddingRow)
            is LoadingViewHolder -> holder.bind()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any?>) {
        when (holder) {
            is EpisodeViewHolder -> bindEpisodeViewHolder(holder, position, animateMultiSelection = MULTI_SELECT_TOGGLE_PAYLOAD in payloads)
            else -> super.onBindViewHolder(holder, position, payloads)
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

    private fun bindEpisodeViewHolder(
        holder: EpisodeViewHolder,
        position: Int,
        animateMultiSelection: Boolean,
    ) {
        val episode = getItem(position) as? PodcastEpisode ?: return
        holder.bind(
            item = episode,
            isMultiSelectEnabled = multiSelectEpisodesHelper.isMultiSelecting,
            isSelected = multiSelectEpisodesHelper.isSelected(episode),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.Podcasts),
            streamByDefault = settings.streamingMode.value,
            animateMultiSelection = animateMultiSelection,
        )
    }

    private fun bindEpisodeLimitRow(holder: EpisodeLimitViewHolder, position: Int) {
        val episodeLimitRow = getItem(position) as? EpisodeLimitRow ?: return
        val limit = episodeLimitRow.episodeLimit
        holder.lblTitle.text = holder.itemView.resources.getString(LR.string.podcast_episodes_limited, limit)
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
        val isHtmlDescription = podcast.podcastHtmlDescription.isNotEmpty()
        val rawDescription = if (isHtmlDescription) {
            podcast.podcastHtmlDescription
        } else {
            podcast.podcastDescription
        }
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
            add(TabsHeader(selectedTab = PodcastTab.EPISODES, onTabClicked = onTabClicked))
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
                        EmptyList(
                            title = context.getString(LR.string.podcast_no_episodes_found),
                            subtitle = context.getString(LR.string.podcast_no_episodes),
                            iconResourceId = IR.drawable.ic_exclamation_circle,
                        ),
                    )
                } else {
                    content.add(
                        EmptyList(
                            title = context.getString(LR.string.podcast_no_episodes_found),
                            subtitle = context.getString(LR.string.podcast_no_episodes_all_archived, archivedCount),
                            iconResourceId = IR.drawable.ic_exclamation_circle,
                            buttonText = context.getString(LR.string.show_archived),
                            onButtonClick = onShowArchivedClicked,
                        ),
                    )
                }
            } else {
                content.add(
                    EmptyList(
                        title = context.getString(LR.string.podcast_no_episodes_found),
                        subtitle = context.getString(LR.string.podcast_no_episodes_matching),
                        iconResourceId = IR.drawable.ic_exclamation_circle,
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
        context: Context,
    ) {
        val content = mutableListOf<Any>().apply {
            add(Podcast())
            add(TabsHeader(selectedTab = PodcastTab.BOOKMARKS, onTabClicked = onTabClicked))

            if (!bookmarksAvailable) {
                add(BookmarkUpsell)
            } else if (searchTerm.isEmpty() && bookmarks.isEmpty()) {
                val resources = context.resources
                add(
                    EmptyList(
                        title = resources.getString(LR.string.bookmarks_empty_state_title),
                        subtitle = resources.getString(LR.string.bookmarks_paid_user_empty_state_message),
                        iconResourceId = IR.drawable.ic_bookmark,
                        buttonText = resources.getString(LR.string.bookmarks_headphone_settings),
                        onButtonClick = onHeadsetSettingsClicked,
                    ),
                )
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
                        EmptyList(
                            title = context.getString(LR.string.podcast_no_bookmarks_found),
                            subtitle = context.getString(LR.string.podcast_no_bookmarks_matching),
                            iconResourceId = IR.drawable.ic_bookmark,
                        ),
                    )
                } else {
                    addAll(
                        bookmarks.map {
                            BookmarkItemData(
                                bookmark = it,
                                episode = episodes.find { episode -> episode.uuid == it.episodeUuid } ?: noOpEpisode,
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

    fun setRecommendations(result: RecommendationsResult) {
        val content = buildList {
            add(Podcast())
            add(TabsHeader(selectedTab = PodcastTab.RECOMMENDATIONS, onTabClicked = onTabClicked))
            when (result) {
                is RecommendationsResult.Loading -> {
                    add(PaddingRow(32.dp))
                    add(LoadingRow)
                    add(PaddingRow(12.dp))
                }

                is RecommendationsResult.Empty -> {
                    val resources = context.resources
                    add(
                        EmptyList(
                            title = resources.getString(LR.string.you_might_like_empty_title),
                            iconResourceId = IR.drawable.ic_exclamation_circle,
                            buttonText = resources.getString(LR.string.you_might_like_empty_button),
                            onButtonClick = onRecommendedRetryClicked,
                        ),
                    )
                }

                is RecommendationsResult.Success -> {
                    val resources = context.resources
                    val list = result.listFeed
                    // Podroll
                    val podroll = list.podroll
                    if (!podroll.isNullOrEmpty()) {
                        add(
                            DividerSubTitleRow(
                                icon = IR.drawable.ic_author_small,
                                title = resources.getString(LR.string.recommended_by_creator),
                                onClick = onPodrollHeaderClicked,
                            ),
                        )
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
                    }
                    // Recommended "You might like" podcasts
                    val podcasts = list.podcasts
                    if (!podcasts.isNullOrEmpty()) {
                        add(
                            DividerSubTitleRow(
                                icon = IR.drawable.ic_duplicate,
                                title = resources.getString(LR.string.similar_shows_to, podcast.title),
                            ),
                        )
                        podcasts.forEachIndexed { index, podcast ->
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
            is DividerRow -> R.layout.adapter_divider_row
            is TabsHeader -> VIEW_TYPE_TABS
            is BookmarkItemData -> VIEW_TYPE_BOOKMARKS
            is BookmarkHeader -> VIEW_TYPE_BOOKMARK_HEADER
            is BookmarkUpsell -> VIEW_TYPE_BOOKMARK_UPSELL
            is EmptyList -> VIEW_TYPE_EMPTY_LIST
            is RecommendedPodcast -> VIEW_TYPE_RECOMMENDED_PODCAST
            is DividerSubTitleRow -> VIEW_TYPE_DIVIDER_SUBTITLE
            is PaddingRow -> VIEW_TYPE_PADDING_ROW
            is LoadingRow -> VIEW_TYPE_LOADING_ROW
            else -> R.layout.adapter_episode
        }
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return when (item) {
            is Podcast -> Long.MAX_VALUE
            is EpisodeHeader -> Long.MAX_VALUE - 1
            is EpisodeLimitRow -> Long.MAX_VALUE - 2
            is TabsHeader -> Long.MAX_VALUE - 4
            is BookmarkHeader -> Long.MAX_VALUE - 5
            is BookmarkUpsell -> Long.MAX_VALUE - 6
            is EmptyList -> Long.MAX_VALUE - 7
            is DividerRow -> item.groupIndex.toLong()
            is PodcastEpisode -> item.adapterId
            is BookmarkItemData -> item.bookmark.adapterId
            is RecommendedPodcast -> item.podcast.adapterId
            is DividerSubTitleRow -> Long.MAX_VALUE - 8
            is PaddingRow -> Long.MAX_VALUE - 9
            is LoadingRow -> Long.MAX_VALUE - 10
            else -> throw IllegalStateException("Unknown item type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EpisodeViewHolder) {
            holder.unbind()
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
                            top = statusBarPadding + 56.dp, // Eyeball the position below app bar
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

    private val noOpEpisode = PodcastEpisode(uuid = "", publishedDate = Date())
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
