package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.transition.doOnEnd
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterUserEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressUpdate
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UploadProgressManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.RowSwipeable
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

class UserEpisodeViewHolder(
    val binding: AdapterUserEpisodeBinding,
    val viewMode: ViewMode,
    val downloadProgressUpdates: Observable<DownloadProgressUpdate>,
    val playbackStateUpdates: Observable<PlaybackState>,
    val upNextChangesObservable: Observable<UpNextQueue.State>,
    val imageLoader: PodcastImageLoader? = null
) : RecyclerView.ViewHolder(binding.root), RowSwipeable {
    override val episodeRow: ViewGroup
        get() = binding.episodeRow
    override val swipeLeftIcon: ImageView
        get() = binding.archiveIcon
    override val leftRightIcon1: ImageView
        get() = binding.leftRightIcon1
    override val leftRightIcon2: ImageView
        get() = binding.leftRightIcon2
    override val episode: Episode?
        get() = binding.episode
    override val positionAdapter: Int
        get() = bindingAdapterPosition
    override val rightToLeftSwipeLayout: ViewGroup
        get() = binding.rightToLeftSwipeLayout
    override val leftToRightSwipeLayout: ViewGroup
        get() = binding.leftToRightSwipeLayout

    sealed class ViewMode {
        object NoArtwork : ViewMode()
        object Artwork : ViewMode()
    }

    val dateFormatter = RelativeDateFormatter(context)
    val context: Context
        get() = binding.root.context
    private val disposables = CompositeDisposable()
    override var upNextAction = Settings.UpNextAction.PLAY_NEXT
    override var isMultiSelecting: Boolean = false
    var uploadConsumer = BehaviorRelay.create<Float>()
    var lastImageLoaded: String? = null

    override val leftIconDrawablesRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() {
            return if (binding.inUpNext == true) {
                listOf(EpisodeItemTouchHelper.IconWithBackground(IR.drawable.ic_upnext_remove, binding.episodeRow.context.getThemeColor(UR.attr.support_05)))
            } else {
                val addToUpNextIcon = when (upNextAction) {
                    Settings.UpNextAction.PLAY_NEXT -> IR.drawable.ic_upnext_playnext
                    Settings.UpNextAction.PLAY_LAST -> IR.drawable.ic_upnext_playlast
                }
                val secondaryUpNextIcon = when (upNextAction) {
                    Settings.UpNextAction.PLAY_NEXT -> IR.drawable.ic_upnext_playlast
                    Settings.UpNextAction.PLAY_LAST -> IR.drawable.ic_upnext_playnext
                }

                listOf(
                    EpisodeItemTouchHelper.IconWithBackground(addToUpNextIcon, binding.episodeRow.context.getThemeColor(UR.attr.support_04)),
                    EpisodeItemTouchHelper.IconWithBackground(secondaryUpNextIcon, binding.episodeRow.context.getThemeColor(UR.attr.support_03))
                )
            }
        }
    override val rightIconDrawableRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() {
            return if (episode?.isArchived == true)
                listOf(EpisodeItemTouchHelper.IconWithBackground(VR.drawable.ic_delete, binding.episodeRow.context.getThemeColor(UR.attr.support_05)))
            else
                listOf(EpisodeItemTouchHelper.IconWithBackground(VR.drawable.ic_delete, binding.episodeRow.context.getThemeColor(UR.attr.support_05)))
        }

    fun setup(episode: UserEpisode, tintColor: Int, playButtonListener: PlayButton.OnClickListener, streamByDefault: Boolean, upNextAction: Settings.UpNextAction, multiSelectEnabled: Boolean = false, isSelected: Boolean = false) {
        this.upNextAction = upNextAction
        this.isMultiSelecting = multiSelectEnabled

        val playButtonType = PlayButton.calculateButtonType(episode, streamByDefault)
        binding.playButtonType = playButtonType
        binding.episode = episode
        binding.tintColor = tintColor
        binding.publishedDate = dateFormatter.format(episode.publishedDate)
        binding.playButton.listener = playButtonListener
        binding.executePendingBindings()

        val captionColor = context.getThemeColor(UR.attr.primary_text_02)
        val captionWithAlpha = ColorUtils.colorWithAlpha(captionColor, 128)

        binding.fileStatusIconsView.setup(episode, downloadProgressUpdates, playbackStateUpdates, upNextChangesObservable)

        val downloadUpdates = downloadProgressUpdates
            .filter { it.episodeUuid == episode.uuid }
            .map { it.downloadProgress }
            .startWith(0f)

        UploadProgressManager.observeUploadProgress(episode.uuid, uploadConsumer)
        val uploadUpdates = uploadConsumer.sample(1, TimeUnit.SECONDS).startWith(0f).doOnDispose { UploadProgressManager.stopObservingUpload(episode.uuid, uploadConsumer) }
        val combinedProgress = Observables.combineLatest(downloadUpdates, uploadUpdates) { first, second ->
            EpisodeStreamProgress(first, second)
        }

        val isInUpNextObservable = upNextChangesObservable.containsUuid(episode.uuid)

        val playbackStateForThisEpisode = playbackStateUpdates
            .startWith(PlaybackState(episodeUuid = episode.uuid)) // Pre load with a blank state so it doesn't wait for the first update
            .filter { it.episodeUuid == episode.uuid } // We only care about playback for this row

        val dateTextView = binding.date
        val titleTextView = binding.title
        val artworkImageView = binding.imgArtwork

        disposables.clear()
        Observables.combineLatest(combinedProgress, playbackStateForThisEpisode, isInUpNextObservable)
            .distinctUntilChanged()
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate { UploadProgressManager.stopObservingUpload(episode.uuid, uploadConsumer) }
            .doOnNext { (_, playbackState, isInUpNext) ->
                episode.playing = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid
                binding.playButtonType = PlayButton.calculateButtonType(episode, streamByDefault)
                binding.inUpNext = isInUpNext

                val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

                val dateTintColor = if (episodeGreyedOut) captionWithAlpha else tintColor
                val dateTextColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(UR.attr.primary_text_02)
                dateTextView.text = episode.getSummaryText(dateFormatter = dateFormatter, tintColor = dateTintColor, showDuration = false, context = dateTextView.context)
                dateTextView.setTextColor(dateTextColor)

                val textColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(UR.attr.primary_text_01)
                titleTextView.setTextColor(textColor)

                binding.executePendingBindings()

                val status = binding.fileStatusIconsView.statusText
                episodeRow.contentDescription = "${titleTextView.text} ${dateTextView.text} $status"

                artworkImageView.alpha = if (episodeGreyedOut) 0.5f else 1f
            }
            .subscribe()
            .addTo(disposables)

        val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
        val dateTintColor = if (episodeGreyedOut) captionWithAlpha else tintColor
        val dateTextColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(UR.attr.primary_text_02)
        dateTextView.text = episode.getSummaryText(dateFormatter = dateFormatter, tintColor = dateTintColor, showDuration = false, context = dateTextView.context)
        dateTextView.setTextColor(dateTextColor)

        val textColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(UR.attr.primary_text_01)
        titleTextView.setTextColor(textColor)

        titleTextView.text = episode.title

        val artworkVisible = viewMode is ViewMode.Artwork
        artworkImageView.isVisible = artworkVisible
        val artworkUrl = episode.getUrlForArtwork(thumbnail = true)
        if (lastImageLoaded != artworkUrl && artworkVisible && imageLoader != null) {
            imageLoader.load(episode, thumbnail = true).into(artworkImageView)
            lastImageLoaded = artworkUrl
        }

        val checkbox = binding.checkbox
        if (checkbox.isVisible != multiSelectEnabled) {
            val transition = AutoTransition()
            transition.duration = 100

            if (!multiSelectEnabled) {
                transition.doOnEnd {
                    binding.playButton.visibility = View.VISIBLE
                    binding.playButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        // Adjust the spacing of the play button to avoid line wrapping when turning on multiselect
                        rightMargin = if (multiSelectEnabled) -checkbox.marginLeft else 0.dpToPx(context)
                        width = 52.dpToPx(context)
                    }
                }
                TransitionManager.beginDelayedTransition(episodeRow, transition) // Have to call this after the listener is set
            } else {
                TransitionManager.beginDelayedTransition(episodeRow, transition)
                binding.playButton.visibility = View.INVISIBLE
                binding.playButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    // Adjust the spacing of the play button to avoid line wrapping when turning on multiselect
                    rightMargin = if (multiSelectEnabled) -checkbox.marginLeft else 0.dpToPx(context)
                    width = 16.dpToPx(context)
                }
            }

            checkbox.isVisible = multiSelectEnabled
            checkbox.setOnClickListener { episodeRow.performClick() }
        }

        val selectedColor = context.getThemeColor(UR.attr.primary_ui_02_selected)
        val unselectedColor = context.getThemeColor(UR.attr.primary_ui_02)
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            binding.episodeRow.setBackgroundColor(if (isMultiSelecting && isChecked) selectedColor else unselectedColor)
        }
        binding.episodeRow.setBackgroundColor(if (isMultiSelecting && isSelected) selectedColor else unselectedColor)
        checkbox.isChecked = isSelected
    }

    fun clearObservers() {
        binding.fileStatusIconsView.clearObservers()

        disposables.clear()
        binding.playButton.listener = null
        uploadConsumer.accept(0.0f)
    }
}

private data class EpisodeStreamProgress(
    val downloadProgress: Float,
    val uploadProgress: Float
)
