package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadge
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadgeStyle
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.adapter.FolderItemDiffCallback
import au.com.shiftyjelly.pocketcasts.views.adapter.PodcastTouchCallback
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.inflate
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class FolderAdapter(
    val clickListener: ClickListener,
    val settings: Settings,
    val context: Context,
    val theme: Theme,
) : ListAdapter<FolderItem, RecyclerView.ViewHolder>(FolderItemDiffCallback()) {

    var badgeType = BadgeType.OFF

    private val imageRequestFactory = PocketCastsImageRequestFactory(context, placeholderType = PlaceholderType.None).themed()
    private var podcastUuidToBadge: Map<String, Int> = emptyMap()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).adapterId
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FolderItem.Podcast -> FolderItem.Podcast.viewTypeId
            is FolderItem.Folder -> FolderItem.Folder.viewTypeId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            FolderItem.Podcast.viewTypeId -> {
                val isLayoutListView = settings.podcastGridLayout.value == PodcastGridLayoutType.LIST_VIEW
                val layoutId = if (isLayoutListView) R.layout.adapter_podcast_list else R.layout.adapter_podcast_grid
                val view = parent.inflate(layoutId, attachToThis = false)
                val podcastGridLayout = settings.podcastGridLayout.value
                PodcastViewHolder(
                    view,
                    imageRequestFactory.copy(
                        cornerRadius = if (FeatureFlag.isEnabled(Feature.PODCASTS_GRID_VIEW_DESIGN_CHANGES)) 4 else 0,
                    ),
                    podcastGridLayout,
                    theme,
                )
            }

            FolderItem.Folder.viewTypeId -> {
                val podcastsLayout = settings.podcastGridLayout.value
                val gridWidthDp = UiUtil.getGridImageWidthPx(smallArtwork = podcastsLayout == PodcastGridLayoutType.SMALL_ARTWORK, context = context).pxToDp(parent.context).toInt()
                FolderViewHolder(
                    composeView = ComposeView(parent.context),
                    theme = theme,
                    gridWidthDp = gridWidthDp,
                    podcastsLayout = podcastsLayout,
                    onFolderClick = { clickListener.onFolderClick(it.uuid, isUserInitiated = true) },
                )
            }

            else -> throw Exception("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FolderItem.Podcast -> (holder as PodcastViewHolder).bind(item.podcast, badgeType, podcastUuidToBadge, clickListener)
            is FolderItem.Folder -> (holder as FolderViewHolder).bind(item.folder, item.podcasts, badgeType, podcastUuidToBadge)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is PodcastViewHolder -> {
                if (context is FragmentActivity && !context.isDestroyed) {
                    holder.podcastThumbnail.setImageDrawable(null)
                }
            }
        }
    }

    fun setBadges(podcastUuidToBadge: Map<String, Int>) {
        // don't update if nothing has changed
        var changes = false
        if (this.podcastUuidToBadge.size != podcastUuidToBadge.size) {
            changes = true
        } else {
            podcastUuidToBadge.entries.forEach { (key, value) ->
                val currentCount = this.podcastUuidToBadge[key]
                if (currentCount == null || currentCount != value) {
                    changes = true
                }
            }
        }
        if (!changes) {
            return
        }
        this.podcastUuidToBadge = podcastUuidToBadge
        applyBadgesToExistingPodcasts()
        notifyDataSetChanged()
    }

    fun setFolderItems(items: List<FolderItem>) {
        applyBadgesToNewPodcasts(items.mapNotNull { (it as? FolderItem.Podcast)?.podcast })
        submitList(items)
    }

    private fun applyBadgesToExistingPodcasts() {
        for (index in 0 until itemCount) {
            val folderItem = getItem(index) as? FolderItem.Podcast ?: continue
            val podcast = folderItem.podcast
            val badge = podcastUuidToBadge[podcast.uuid]
            if (badge != null && badge != podcast.unplayedEpisodeCount) {
                podcast.unplayedEpisodeCount = badge
            }
        }
    }

    private fun applyBadgesToNewPodcasts(podcasts: List<Podcast>) {
        podcasts.forEach { podcast ->
            podcastUuidToBadge[podcast.uuid]?.let { podcast.unplayedEpisodeCount = it }
        }
    }

    class PodcastViewHolder(
        val view: View,
        private val imageRequestFactory: PocketCastsImageRequestFactory,
        podcastGridLayout: PodcastGridLayoutType,
        val theme: Theme,
    ) : RecyclerView.ViewHolder(view), PodcastTouchCallback.ItemTouchHelperViewHolder {

        val button: View = view.findViewById(R.id.button)
        val podcastThumbnail: ImageView = view.findViewById(R.id.podcast_artwork)
        val podcastCardView = view.findViewById<CardView>(R.id.podcast_card_view)
        val podcastBackground: View? = view.findViewById(R.id.header_background)
        val podcastTitle: TextView = view.findViewById(R.id.library_podcast_title)
        val author: TextView? = view.findViewById(R.id.podcast_author)
        val unplayedText: TextView = view.findViewById(R.id.unplayed_count)
        val unplayedBackground: ImageView? = view.findViewById(R.id.unplayed_background)
        val countTextMarginSmall: Int = 2.dpToPx(view.resources.displayMetrics)
        val cardElevation: Float = 1.dpToPx(view.resources.displayMetrics).toFloat()
        val cardCornerRadius: Float = 4.dpToPx(view.resources.displayMetrics).toFloat()
        val countTextMarginLarge: Int = 4.dpToPx(view.resources.displayMetrics)
        val isListLayout: Boolean = podcastGridLayout == PodcastGridLayoutType.LIST_VIEW
        val unplayedCountBadgeView = view.findViewById<ComposeView>(R.id.badge_view)

        fun bind(podcast: Podcast, badgeType: BadgeType, podcastUuidToBadge: Map<String, Int>, clickListener: ClickListener) {
            button.setOnClickListener { clickListener.onPodcastClick(podcast, itemView) }
            podcastCardView?.setOnClickListener { clickListener.onPodcastClick(podcast, itemView) }
            podcastTitle.text = podcast.title
            podcastTitle.show()
            author?.text = podcast.author
            if (!isListLayout) {
                UiUtil.setBackgroundColor(podcastTitle, ColorManager.getBackgroundColor(podcast))
            }
            val unplayedEpisodeCount = podcastUuidToBadge[podcast.uuid] ?: 0
            val badgeCount = when (badgeType) {
                BadgeType.OFF -> 0
                BadgeType.ALL_UNFINISHED -> unplayedEpisodeCount
                BadgeType.LATEST_EPISODE -> min(1, unplayedEpisodeCount)
            }
            val displayBadgeCount = if (badgeCount > 99) 99 else badgeCount
            if (FeatureFlag.isEnabled(Feature.PODCASTS_GRID_VIEW_DESIGN_CHANGES)) {
                unplayedText.hide()
                unplayedCountBadgeView?.let {
                    it.setBadgeContent(displayBadgeCount, badgeType)
                    it.show()
                }
                podcastCardView?.elevation = cardElevation
                podcastCardView?.radius = cardCornerRadius
            } else {
                unplayedText.show()
                setTextViewCount(unplayedBackground, unplayedText, displayBadgeCount, badgeType)

                if (!isListLayout) {
                    unplayedText.setTextColor(unplayedText.context.getThemeColor(UR.attr.contrast_01))
                    podcastCardView?.elevation = 0f
                    podcastCardView?.radius = 0f
                } else {
                    if (badgeType == BadgeType.LATEST_EPISODE) {
                        unplayedText.setTextColor(unplayedText.context.getThemeColor(UR.attr.support_05))
                    } else {
                        unplayedText.setTextColor(unplayedText.context.getThemeColor(UR.attr.primary_text_02))
                    }
                }
            }

            val badgeCountMessage = if (badgeType == BadgeType.OFF) "" else "$unplayedEpisodeCount new episodes. "
            button.contentDescription = "${podcast.title}. $badgeCountMessage Open podcast."

            imageRequestFactory
                .create(podcast, onSuccess = { if (!isListLayout) podcastTitle.hide() })
                .loadInto(podcastThumbnail)
        }

        private fun ComposeView.setBadgeContent(
            count: Int,
            badgeType: BadgeType,
        ) {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                AppTheme(theme.activeTheme) {
                    CountBadge(
                        count = count,
                        style = when {
                            (badgeType == BadgeType.LATEST_EPISODE) -> CountBadgeStyle.Small
                            isListLayout -> CountBadgeStyle.Big
                            else -> CountBadgeStyle.Medium
                        },
                        modifier = if (isListLayout) {
                            Modifier
                        } else {
                            Modifier
                                .wrapContentSize(align = Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                        },
                    )
                }
            }
        }

        @Suppress("NAME_SHADOWING")
        private fun setTextViewCount(image: ImageView?, text: TextView, count: Int, badgeType: BadgeType) {
            if (count == 0) {
                text.hide()
                image?.hide()
            } else {
                text.show()
                image?.show()
                if (!isListLayout) {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (count > 9) 12f else 14f)
                    (text.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, if (count > 9) countTextMarginSmall else countTextMarginLarge, 0)
                }

                if (badgeType != BadgeType.LATEST_EPISODE) {
                    text.text = count.toString()
                } else {
                    text.text = "●"
                }
            }
        }

        override fun onItemDrag() {
            AnimatorSet().apply {
                val elevation = ObjectAnimator.ofPropertyValuesHolder(itemView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 16.dpToPx(itemView.resources.displayMetrics).toFloat()))
                play(elevation)
                start()
            }
        }

        override fun onItemClear() {
            itemView.background = null
            AnimatorSet().apply {
                val elevation = ObjectAnimator.ofPropertyValuesHolder(itemView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 0.toFloat()))
                play(elevation)
                start()
            }
        }
    }

    interface ClickListener {
        fun onPodcastClick(podcast: Podcast, view: View)
        fun onFolderClick(folderUuid: String, isUserInitiated: Boolean)
    }
}
