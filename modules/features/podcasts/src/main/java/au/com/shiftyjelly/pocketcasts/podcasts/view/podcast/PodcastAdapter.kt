package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
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
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterPodcastHeaderBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings.PodcastRatingRow
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkHeaderViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkUpsellViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.BookmarkViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.NoBookmarkViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter.TabsViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingTappedSource
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.extensions.toggleVisibility
import au.com.shiftyjelly.pocketcasts.views.helper.AnimatorUtil
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.toCircle
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.Date
import timber.log.Timber
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
    private val headerType: PodcastAdapter.HeaderType,
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
    private val onNotificationsClicked: () -> Unit,
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
    private val onChangeHeaderExpanded: (String, Boolean) -> Unit,
    private val onClickRating: (String, RatingTappedSource) -> Unit,
    private val onArtworkAvailable: (String) -> Unit,
    private val sourceView: SourceView,
) : LargeListAdapter<Any, RecyclerView.ViewHolder>(1500, differ) {

    data class EpisodeLimitRow(val episodeLimit: Int)
    class DividerRow(val grouping: PodcastGrouping, val groupIndex: Int)
    data class NoResultsMessage(val title: String, val bodyText: String, val showButton: Boolean)
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
    object NoBookmarkMessage

    enum class HeaderType {
        SolidColor,
        Blur,
        Scrim,
    }

    companion object {
        private const val VIEW_TYPE_TABS = 100
        private const val VIEW_TYPE_BOOKMARKS = 101
        private const val VIEW_TYPE_BOOKMARK_HEADER = 102
        private const val VIEW_TYPE_BOOKMARK_UPSELL = 103
        private const val VIEW_TYPE_NO_BOOKMARK = 104
        val VIEW_TYPE_EPISODE_HEADER = R.layout.adapter_episode_header
        val VIEW_TYPE_PODCAST_HEADER = R.layout.adapter_podcast_header
        val VIEW_TYPE_EPISODE_LIMIT_ROW = R.layout.adapter_episode_limit
        val VIEW_TYPE_NO_RESULTS = R.layout.adapter_no_results
        val VIEW_TYPE_DIVIDER = R.layout.adapter_divider_row
    }

    private val disposables = CompositeDisposable()
    private var podcast: Podcast = Podcast()

    private var headerExpanded: Boolean = false
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
            VIEW_TYPE_PODCAST_HEADER -> when (headerType) {
                HeaderType.SolidColor -> PodcastViewHolder(AdapterPodcastHeaderBinding.inflate(inflater, parent, false), this)
                HeaderType.Blur, HeaderType.Scrim -> PodcastHeaderViewHolder(
                    context = parent.context,
                    theme = theme,
                    useBlurredArtwork = headerType == HeaderType.Blur,
                    onClickRating = onClickRating,
                    onClickFollow = onSubscribeClicked,
                    onClickUnfollow = { onUnsubscribeClicked { } },
                    onClickFolder = onFoldersClicked,
                    onClickNotification = onNotificationsClicked,
                    onClickSettings = onSettingsClicked,
                    onToggleHeader = {
                        onChangeHeaderExpanded(podcast.uuid, !podcast.isHeaderExpanded)
                    },
                    onLongClickArtwork = {
                        onArtworkLongClicked { notifyItemChanged(0) }
                    },
                    onArtworkAvailable = onArtworkAvailable,
                )
            }

            VIEW_TYPE_TABS -> TabsViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_EPISODE_HEADER -> EpisodeHeaderViewHolder(AdapterEpisodeHeaderBinding.inflate(inflater, parent, false), onEpisodesOptionsClicked, onSearchFocus)
            VIEW_TYPE_EPISODE_LIMIT_ROW -> EpisodeLimitViewHolder(inflater.inflate(R.layout.adapter_episode_limit, parent, false))
            VIEW_TYPE_NO_RESULTS -> NoResultsViewHolder(inflater.inflate(R.layout.adapter_no_results, parent, false))
            VIEW_TYPE_DIVIDER -> DividerViewHolder(inflater.inflate(R.layout.adapter_divider_row, parent, false))
            VIEW_TYPE_BOOKMARKS -> BookmarkViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_HEADER -> BookmarkHeaderViewHolder(ComposeView(parent.context), theme)
            VIEW_TYPE_BOOKMARK_UPSELL -> BookmarkUpsellViewHolder(ComposeView(parent.context), sourceView, theme)
            VIEW_TYPE_NO_BOOKMARK -> NoBookmarkViewHolder(ComposeView(parent.context), theme, onHeadsetSettingsClicked)
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
            is PodcastHeaderViewHolder -> holder.bind(podcast, ratingState, signInState)
            is EpisodeViewHolder -> bindEpisodeViewHolder(holder, position, fromListUuid)
            is PodcastViewHolder -> bindPodcastViewHolder(holder)
            is TabsViewHolder -> holder.bind(getItem(position) as TabsHeader)
            is EpisodeHeaderViewHolder -> bindingEpisodeHeaderViewHolder(holder, position)
            is EpisodeLimitViewHolder -> bindEpisodeLimitRow(holder, position)
            is NoResultsViewHolder -> bindNoResultsMessage(holder, position)
            is DividerViewHolder -> bindDividerRow(holder, position)
            is BookmarkViewHolder -> holder.bind(getItem(position) as BookmarkItemData)
            is BookmarkHeaderViewHolder -> holder.bind(getItem(position) as BookmarkHeader)
            is BookmarkUpsellViewHolder -> holder.bind()
            is NoBookmarkViewHolder -> holder.bind()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

    private fun bindPodcastViewHolder(holder: PodcastViewHolder) {
        bindHeaderBottom(holder, ratingState)
        bindHeaderTop(holder)

        val imageView = holder.binding.top.artwork
        // stopping the artwork flickering when the image is reloaded
        if (imageView.drawable == null || holder.lastImagePodcastUuid == null || holder.lastImagePodcastUuid != podcast.uuid) {
            holder.lastImagePodcastUuid = podcast.uuid
            imageRequestFactory.create(podcast).loadInto(imageView)
        }

        imageView.setOnLongClickListener {
            onArtworkLongClicked {
                imageRequestFactory.create(podcast).loadInto(imageView)
            }
            true
        }

        holder.binding.podcastHeader.contentDescription = podcast.title
    }

    private fun bindHeaderBottom(holder: PodcastViewHolder, ratingState: RatingState) {
        holder.binding.bottom.root.isVisible = headerExpanded

        val isHtmlDescription = FeatureFlag.isEnabled(Feature.PODCAST_HTML_DESCRIPTION) && podcast.podcastHtmlDescription.isNotEmpty()
        val descriptionLinkColor: Int = ThemeColor.podcastText02(theme.activeTheme, tintColor)

        val description = if (isHtmlDescription) {
            HtmlCompat.fromHtml(
                podcast.podcastHtmlDescription,
                HtmlCompat.FROM_HTML_MODE_COMPACT and
                    HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH.inv(), // keep the extra line break from paragraphs as it looks better
            ).toAnnotatedString(urlColor = descriptionLinkColor)
        } else {
            AnnotatedString(podcast.podcastDescription)
        }

        holder.binding.bottom.podcastHeaderBottom.setContent {
            AppTheme(theme.activeTheme) {
                PodcastHeaderBottom(
                    title = podcast.title,
                    category = podcast.getFirstCategory(context.resources),
                    description = description,
                    podcastInfoContent = {
                        PodcastInfoView(
                            PodcastInfoState(
                                author = podcast.author,
                                link = podcast.getShortUrl(),
                                schedule = podcast.displayableFrequency(context.resources),
                                next = podcast.displayableNextEpisodeDate(context),
                            ),
                            onWebsiteLinkClicked = {
                                onWebsiteLinkClicked(context)
                            },
                        )
                    },
                    ratingsContent = {
                        if (ratingState is RatingState.Loaded) {
                            PodcastRatingRow(
                                state = ratingState,
                                onClick = { source -> onClickRating(podcast.uuid, source) },
                            )
                        }
                    },
                    onDescriptionClicked = onPodcastDescriptionClicked,
                )
            }
        }
    }

    private fun bindHeaderTop(holder: PodcastViewHolder) {
        val isPlusOrPatronUser = signInState.isSignedInAsPlusOrPatron
        holder.binding.top.chevron.isEnabled = headerExpanded
        holder.binding.top.settings.isVisible = podcast.isSubscribed
        holder.binding.top.subscribeButton.isVisible = !podcast.isSubscribed
        holder.binding.top.subscribedButton.isVisible = podcast.isSubscribed
        holder.binding.top.subscribedButton.toCircle(true)
        holder.binding.top.header.setBackgroundColor(ThemeColor.podcastUi03(theme.activeTheme, podcast.backgroundColor))
        holder.binding.top.folders.setImageResource(
            when {
                !isPlusOrPatronUser -> IR.drawable.ic_folder_plus
                podcast.folderUuid != null -> IR.drawable.ic_folder_check
                else -> IR.drawable.ic_folder
            },
        )
        holder.binding.top.folders.isVisible = podcast.isSubscribed
        with(holder.binding.top.notifications) {
            val notificationsIconText =
                context.getString(if (podcast.isShowNotifications) LR.string.podcast_notifications_on else LR.string.podcast_notifications_off)
            contentDescription = notificationsIconText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tooltipText = notificationsIconText
            }
            setImageResource(
                if (podcast.isShowNotifications) IR.drawable.ic_notifications_on else IR.drawable.ic_notifications_off,
            )
            isVisible = podcast.isSubscribed
        }
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

    private fun bindDividerRow(holder: DividerViewHolder, position: Int) {
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
            add(TabsHeader(PodcastTab.EPISODES, onTabClicked))
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
        context: Context,
    ) {
        val content = mutableListOf<Any>().apply {
            add(Podcast())
            add(TabsHeader(PodcastTab.BOOKMARKS, onTabClicked))

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

    fun setBookmarksAvailable(bookmarksAvailable: Boolean) {
        this.bookmarksAvailable = bookmarksAvailable
    }

    fun setError() {
        submitList(emptyList())
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is Podcast -> R.layout.adapter_podcast_header
            is EpisodeHeader -> R.layout.adapter_episode_header
            is EpisodeLimitRow -> R.layout.adapter_episode_limit
            is NoResultsMessage -> R.layout.adapter_no_results
            is DividerRow -> R.layout.adapter_divider_row
            is TabsHeader -> VIEW_TYPE_TABS
            is BookmarkItemData -> VIEW_TYPE_BOOKMARKS
            is BookmarkHeader -> VIEW_TYPE_BOOKMARK_HEADER
            is BookmarkUpsell -> VIEW_TYPE_BOOKMARK_UPSELL
            is NoBookmarkMessage -> VIEW_TYPE_NO_BOOKMARK
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
            else -> throw IllegalStateException("Unknown item type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EpisodeViewHolder) {
            holder.clearObservers()
        }
    }

    private fun onHeaderClicked(binding: AdapterPodcastHeaderBinding) {
        val transition = ChangeBounds().apply {
            duration = 200
            interpolator = FastOutSlowInInterpolator()
        }

        val constraintLayout = binding.top.root
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.constrainPercentWidth(R.id.artworkContainer, if (!binding.bottom.root.isVisible) 0.40f else 0.38f)

        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transition)
        constraintSet.applyTo(constraintLayout)

        val expanded = binding.bottom.root.toggleVisibility()
        binding.top.chevron.isEnabled = expanded
        headerExpanded = expanded
        onHeaderSummaryToggled(expanded, true)
    }

    private fun onWebsiteLinkClicked(context: Context) {
        podcast.podcastUrl?.let { url ->
            if (url.isNotBlank()) {
                try {
                    context.startActivity(webUrlToIntent(url), null)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open podcast web page.")
                }
            }
        }
    }

    private fun webUrlToIntent(url: String): Intent {
        var uri = Uri.parse(url)
        // fix for podcast web pages that don't start with http://
        if (uri.scheme.isNullOrBlank() && !url.contains("://")) {
            uri = Uri.parse("http://$url")
        }
        return Intent(Intent.ACTION_VIEW, uri)
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

    internal class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)
    }

    internal inner class PodcastViewHolder(val binding: AdapterPodcastHeaderBinding, val adapter: PodcastAdapter) : RecyclerView.ViewHolder(binding.root) {

        var lastImagePodcastUuid: String? = null

        init {
            binding.top.header.setOnClickListener {
                adapter.onHeaderClicked(binding)
            }
            binding.top.artwork.setOnClickListener {
                adapter.onHeaderClicked(binding)
            }
            binding.top.subscribeButton.setOnClickListener {
                animateToSubscribed()
            }
            binding.top.subscribedButton.setOnClickListener {
                unsubscribe()
            }
            binding.top.folders.setOnClickListener {
                adapter.onFoldersClicked()
            }
            binding.top.notifications.setOnClickListener {
                adapter.onNotificationsClicked()
            }
            binding.top.settings.setOnClickListener {
                adapter.onSettingsClicked()
            }
        }

        private fun unsubscribe() {
            adapter.onUnsubscribeClicked {
                unsubscribeConfirmed()
            }
        }

        private fun unsubscribeConfirmed() {
            val subscribeButton = binding.top.subscribeButton
            val subscribedButton = binding.top.subscribedButton
            val greenButton = binding.top.animationSubscribedButton
            val subscribeText = binding.top.animationSubscribeText
            val notificationsButton = binding.top.notifications
            val settingsButton = binding.top.settings

            subscribeButton.show()
            subscribedButton.hide()
            greenButton.hide()
            subscribeText.hide()
            notificationsButton.hide()
            settingsButton.hide()
        }

        private fun animateToSubscribed() {
            val subscribeButton = binding.top.subscribeButton
            val subscribedButton = binding.top.subscribedButton
            val greenButton = binding.top.animationSubscribedButton
            val subscribeText = binding.top.animationSubscribeText
            val notificationsButton = binding.top.notifications
            val settingsButton = binding.top.settings
            val displayMetrics = greenButton.context.resources.displayMetrics
            val accelerateDecelerateInterpolator = AccelerateDecelerateInterpolator()

            greenButton.alpha = 0f
            greenButton.show()

            subscribeText.alpha = 0f
            subscribeText.show()

            subscribedButton.alpha = 0f
            subscribedButton.show()

            notificationsButton.alpha = 0f
            notificationsButton.show()

            settingsButton.alpha = 0f
            settingsButton.show()

            val fadeInGreenButton = AnimatorUtil.fadeIn(greenButton, 300)
            fadeInGreenButton.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    subscribeButton.hide()
                }
            })

            val fadeInSubscribeText = AnimatorUtil.fadeIn(subscribeText, 300)

            val fadeInButton = AnimatorSet()
            fadeInButton.playTogether(fadeInGreenButton, fadeInSubscribeText)

            val greenButtonWidth = subscribeButton.measuredWidth
            val changeWidthGreenButton = ValueAnimator.ofInt(greenButtonWidth, 32.dpToPx(displayMetrics))
            changeWidthGreenButton.addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Int
                val layoutParams = greenButton.layoutParams
                layoutParams.width = animatedValue
                greenButton.layoutParams = layoutParams
            }
            changeWidthGreenButton.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    greenButton.layoutParams.width = greenButtonWidth
                    greenButton.hide()
                }
            })
            changeWidthGreenButton.duration = 300
            changeWidthGreenButton.startDelay = 600

            val fadeOutSubscribeText = AnimatorUtil.fadeOut(subscribeText, 100)
            fadeOutSubscribeText.startDelay = 600

            val fadeInSubscribedButton = AnimatorUtil.fadeIn(subscribedButton, 200)
            fadeInSubscribedButton.startDelay = 700

            val fadeInNotificationsButton = AnimatorUtil.fadeIn(notificationsButton, 200)
            fadeInNotificationsButton.startDelay = 700

            val fadeInSettingsButton = AnimatorUtil.fadeIn(settingsButton, 200)
            fadeInSettingsButton.startDelay = 700

            val translationXNotificationsButton = AnimatorUtil.translationX(notificationsButton, 100.dpToPx(displayMetrics), 0, accelerateDecelerateInterpolator, 900)
            val translationXSettingsButton = AnimatorUtil.translationX(settingsButton, 100.dpToPx(displayMetrics), 0, accelerateDecelerateInterpolator, 900)

            val widthAndTickSet = AnimatorSet()
            widthAndTickSet.playTogether(fadeOutSubscribeText, changeWidthGreenButton, fadeInSubscribedButton, fadeInNotificationsButton, fadeInSettingsButton, translationXSettingsButton, translationXNotificationsButton)

            val set = AnimatorSet()
            set.playSequentially(fadeInButton, widthAndTickSet)
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (headerExpanded) {
                        adapter.onHeaderClicked(binding)
                    }
                    adapter.onSubscribeClicked()
                }
            })
            set.start()
        }
    }

    private class PodcastHeaderViewHolder(
        context: Context,
        private val theme: Theme,
        private val useBlurredArtwork: Boolean,
        private val onClickRating: (String, RatingTappedSource) -> Unit,
        private val onClickFollow: () -> Unit,
        private val onClickUnfollow: () -> Unit,
        private val onClickFolder: () -> Unit,
        private val onClickNotification: () -> Unit,
        private val onClickSettings: () -> Unit,
        private val onToggleHeader: () -> Unit,
        private val onLongClickArtwork: () -> Unit,
        private val onArtworkAvailable: (String) -> Unit,
    ) : RecyclerView.ViewHolder(ComposeView(context)) {
        private val composeView get() = itemView as ComposeView

        init {
            composeView.setTag(UR.id.podcast_view_header_tag, true)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        fun bind(
            podcast: Podcast,
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
                        rating = ratingState,
                        isFollowed = podcast.isSubscribed,
                        areNotificationsEnabled = podcast.isShowNotifications,
                        folderIcon = when {
                            !signInState.isSignedInAsPlusOrPatron -> PodcastFolderIcon.BuyFolders
                            podcast.folderUuid != null -> PodcastFolderIcon.AddedToFolder
                            else -> PodcastFolderIcon.NotInFolder
                        },
                        isHeaderExpanded = podcast.isHeaderExpanded,
                        contentPadding = PaddingValues(
                            top = statusBarPadding + 40.dp, // Eyeball the position inside app bar
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                        useBlurredArtwork = useBlurredArtwork,
                        onClickRating = { source -> onClickRating(podcast.uuid, source) },
                        onClickFollow = onClickFollow,
                        onClickUnfollow = onClickUnfollow,
                        onClickFolder = onClickFolder,
                        onClickNotification = onClickNotification,
                        onClickSettings = onClickSettings,
                        onToggleHeader = onToggleHeader,
                        onLongClickArtwork = onLongClickArtwork,
                        onArtworkAvailable = { onArtworkAvailable(podcast.uuid) },
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
