package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.ViewPlayerBottomSheetBinding
import au.com.shiftyjelly.pocketcasts.player.helper.BottomSheetAnimation
import au.com.shiftyjelly.pocketcasts.player.helper.BottomSheetAnimation.Companion.SCALE
import au.com.shiftyjelly.pocketcasts.player.helper.BottomSheetAnimation.Companion.SCALE_NORMAL
import au.com.shiftyjelly.pocketcasts.player.helper.BottomSheetAnimation.Companion.TRANSLATE_Y
import au.com.shiftyjelly.pocketcasts.player.helper.PCBottomSheetBehavior
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.isHidden
import au.com.shiftyjelly.pocketcasts.views.extensions.isVisible
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class PlayerBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), CoroutineScope {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = DataBindingUtil.inflate<ViewPlayerBottomSheetBinding>(inflater, R.layout.view_player_bottom_sheet, this, true)
    var sheetBehavior: PlayerBottomSheetBehavior<PlayerBottomSheet>? = null
    private var bottomSheetCallback: PCBottomSheetBehavior.BottomSheetCallback? = null
    private var animations: Array<BottomSheetAnimation>? = null
    private var rootView: CoordinatorLayout? = null

    var isPlayerOpen = false
    var shouldPlayerOpenOnAttach = false
    var hasLoadedFirstTime = false

    var listener: PlayerBottomSheetListener? = null
    var isDragEnabled: Boolean
        get() = sheetBehavior?.isDragEnabled ?: false
        set(value) { sheetBehavior?.isDragEnabled = value }

    init {
        elevation = 8.dpToPx(context).toFloat()

        binding.miniPlayer.clickListener = object : MiniPlayer.OnMiniPlayerClicked {
            override fun onPlayClicked() {
                listener?.onPlayClicked()
            }

            override fun onPauseClicked() {
                listener?.onPauseClicked()
            }

            override fun onSkipBackwardClicked() {
                listener?.onSkipBackwardClicked()
            }

            override fun onSkipForwardClicked() {
                listener?.onSkipForwardClicked()
            }

            override fun onPlayerClicked() {
                openPlayer()
            }

            override fun onUpNextClicked() {
                listener?.onUpNextClicked()
            }

            override fun onLongClick() {
                listener?.onMiniPlayerLongClick()
            }
        }
    }

    interface PlayerBottomSheetListener {
        fun onMiniPlayerHidden()
        fun onMiniPlayerVisible()
        fun onPlayerOpen()
        fun onPlayerBottomSheetSlide(slideOffset: Float)
        fun onPlayerClosed()
        fun onPlayClicked()
        fun onPauseClicked()
        fun onSkipBackwardClicked()
        fun onSkipForwardClicked()
        fun onUpNextClicked()
        fun onMiniPlayerLongClick()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        rootView = parent as CoordinatorLayout

        binding.player.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

        bottomSheetCallback = createBottomSheetCallback()
        sheetBehavior = (PCBottomSheetBehavior.from(this) as PlayerBottomSheetBehavior).apply {
            addBottomSheetCallback(bottomSheetCallback!!)
        }

        if (shouldPlayerOpenOnAttach) {
            shouldPlayerOpenOnAttach = false

            doOnLayout {
                sheetBehavior?.apply {
                    state = PCBottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    fun setPlaybackState(playbackState: PlaybackState) {
        binding.miniPlayer.setPlaybackState(playbackState)
    }

    fun setUpNext(upNext: UpNextQueue.State, theme: Theme, shouldAnimateOnAttach: Boolean) {
        binding.miniPlayer.setUpNext(upNext, theme)

        // only show the mini player when an episode is loaded
        if (upNext is UpNextQueue.State.Loaded) {
            if ((isHidden() || !hasLoadedFirstTime)) {
                show()
                if (!shouldPlayerOpenOnAttach && shouldAnimateOnAttach) {
                    translationY = 68.dpToPx(context).toFloat()
                    animate().translationY(0f).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            listener?.onMiniPlayerVisible()
                            hasLoadedFirstTime = true
                        }
                    })
                } else {
                    translationY = 0f
                    if (!shouldAnimateOnAttach) {
                        listener?.onMiniPlayerVisible()
                        hasLoadedFirstTime = true
                    }
                }
            }
        } else {
            if (isVisible()) {
                hide()
                closePlayer()
                listener?.onMiniPlayerHidden()
            }
        }
    }

    private fun createBottomSheetCallback(): PCBottomSheetBehavior.BottomSheetCallback {
        val miniPlayButtonScale = BottomSheetAnimation(
            viewId = R.id.miniPlayButton,
            rootView = rootView,
            effect = SCALE,
            slideOffsetFrom = 0.2f,
            slideOffsetTo = 0.5f,
            valueFrom = SCALE_NORMAL,
            valueTo = 0.6f,
            closeStartDelay = 200,
            closeInterpolator = OvershootInterpolator(),
            disabled = false
        )
        val playButtonScale = BottomSheetAnimation(
            viewId = R.id.largePlayButton,
            rootView = rootView,
            effect = SCALE,
            slideOffsetFrom = 0.6f,
            slideOffsetTo = 0.9f,
            valueFrom = 0.6f,
            valueTo = SCALE_NORMAL,
            openStartDelay = 200,
            openInterpolator = OvershootInterpolator(),
            disabled = false
        )
        val backgroundScale = BottomSheetAnimation(
            viewId = R.id.container,
            rootView = rootView,
            effect = SCALE,
            slideOffsetFrom = 0f,
            slideOffsetTo = 1f,
            valueFrom = SCALE_NORMAL,
            valueTo = 0.9f,
            disabled = false
        )
        val playerTranslateY = BottomSheetAnimation(
            viewId = R.id.player,
            rootView = rootView,
            effect = TRANSLATE_Y,
            slideOffsetFrom = 0f,
            slideOffsetTo = 1f,
            valueFrom = resources.getDimension(R.dimen.player_fragment_start_y),
            valueTo = 0f,
            disabled = false
        )
        animations = arrayOf(miniPlayButtonScale, playButtonScale, backgroundScale, playerTranslateY)

        return object : PCBottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // remove bottom navigation view
                listener?.onPlayerBottomSheetSlide(slideOffset)

                animations?.forEach { it.onSlide(slideOffset) }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> onCollapsed()
                    BottomSheetBehavior.STATE_DRAGGING -> onDragging()
                    BottomSheetBehavior.STATE_SETTLING -> onSettling()
                    BottomSheetBehavior.STATE_EXPANDED -> onExpanded()
                }
            }
        }
    }

    private fun onCollapsed() {
        isPlayerOpen = false
        listener?.onPlayerClosed()
        animations?.forEach { it.onCollapsed() }
        analyticsTracker.track(AnalyticsEvent.PLAYER_DISMISSED)

        binding.player.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        binding.miniPlayer.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun onDragging() {
        animations?.forEach { it.onDragging() }
    }

    private fun onSettling() {
        animations?.forEach { it.onSettling() }
    }

    private fun onExpanded() {
        isPlayerOpen = true
        listener?.onPlayerOpen()
        animations?.forEach { it.onExpanded() }
        analyticsTracker.track(AnalyticsEvent.PLAYER_SHOWN)

        binding.player.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.miniPlayer.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    fun openPlayer() {
        doOnLayout {
            // If bottom sheet isn't laid out yet it will never call it's callback
            // Seems like a bug in bottom sheet but not sure.
            // Just to be sure we wrap this in doOnLayout
            sheetBehavior?.let { bottomSheet ->
                bottomSheet.state = PCBottomSheetBehavior.STATE_EXPANDED
            }

            if (sheetBehavior == null) {
                shouldPlayerOpenOnAttach = true
            }
        }
    }

    fun closePlayer() {
        isPlayerOpen = false
        sheetBehavior?.state = PCBottomSheetBehavior.STATE_COLLAPSED
    }
}

open class PlayerBottomSheetBehavior<V : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PCBottomSheetBehavior<V>(context, attrs) {
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (!isDragEnabled) return false
        return super.onInterceptTouchEvent(parent, child, event)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (!isDragEnabled) return false
        return super.onTouchEvent(parent, child, event)
    }
}
