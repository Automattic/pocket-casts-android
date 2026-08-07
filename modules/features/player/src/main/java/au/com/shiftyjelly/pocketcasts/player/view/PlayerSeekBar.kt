package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.toHhMmSs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PlayerSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val view = inflater.inflate(R.layout.view_playback_seek_bar, this)
    private var episodeUuid = ""
    private var nextSeekSessionId = 0L
    private var seekSession: SeekSession? = null
    private var pendingNonTouchCommit: Runnable? = null
    private var touchSeeking = false
    private var ignoreTouchProgressUntilNextStart = false
    private var currentTime = Duration.ZERO
    private var duration = Duration.ZERO
    private var chapters = Chapters()
    private var playbackSpeed = 1.0
    private var adjustDuration = false
    private val seekBar = view.findViewById(R.id.seekBarInternal) as AppCompatSeekBar
    private val elapsedTimeText = view.findViewById(R.id.elapsedTime) as TextView
    private val remainingTimeText = view.findViewById(R.id.remainingTime) as TextView
    private val bufferingSeekbar = view.findViewById(R.id.indeterminateProgressBar) as MaterialProgressBar

    var bufferedUpToInSecs: Int = 0
        set(value) {
            field = value
            seekBar.secondaryProgress = value
        }

    var isBuffering: Boolean = false
        set(value) {
            field = value
            bufferingSeekbar.isVisible = value
        }

    var changeListener: OnUserSeekListener? = null

    init {
        setupSeekBar()
    }

    fun setAdjustDuration(adjust: Boolean) {
        this.adjustDuration = adjust
        updateTextViews()
    }

    fun setEpisodeUuid(episodeUuid: String) {
        if (this.episodeUuid == episodeUuid) {
            return
        }
        finishSeekForLifecycleChange()
        this.episodeUuid = episodeUuid
    }

    fun setCurrentTime(currentTime: Duration) {
        if (seekSession != null) {
            return
        }
        this.currentTime = currentTime
        seekBar.progress = currentTime.inWholeSeconds.toInt()
        updateTextViews()
    }

    fun setChapters(chapters: Chapters) {
        if (seekSession != null) {
            return
        }
        this.chapters = chapters
        updateTextViews()
    }

    fun setDuration(duration: Duration) {
        this.duration = duration
        seekBar.max = duration.inWholeSeconds.toInt()
        updateTextViews()
    }

    fun setPlaybackSpeed(playbackSpeed: Double) {
        this.playbackSpeed = playbackSpeed
        updateTextViews()
    }

    fun setTintColor(color: Int?, theme: Theme.ThemeType) {
        val themeColor = ThemeColor.playerHighlight01(theme, color ?: Color.WHITE)
        seekBar.thumbTintList = ColorStateList.valueOf(ThemeColor.playerContrast01(theme))
        seekBar.progressTintList = ColorStateList.valueOf(ThemeColor.playerContrast01(theme))
        seekBar.secondaryProgressTintList = ColorStateList.valueOf(ThemeColor.playerContrast05(theme))
        seekBar.backgroundTintList = ColorStateList.valueOf(ThemeColor.playerContrast05(theme))

        with(bufferingSeekbar) {
            supportIndeterminateTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(themeColor, 0x1A))
        }

        elapsedTimeText.setTextColor(ThemeColor.playerContrast02(theme))
        remainingTimeText.setTextColor(ThemeColor.playerContrast02(theme))
    }

    private fun setupSeekBar() {
        bufferingSeekbar.isVisible = false
        seekBar.secondaryProgress = 0
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (ignoreTouchProgressUntilNextStart) {
                    ignoreTouchProgressUntilNextStart = false
                    touchSeeking = false
                    return
                }
                touchSeeking = false
                seekSession
                    ?.takeIf { it.phase == SeekPhase.Touch }
                    ?.let { commitSeek(it.id, it.target, it.maxAtInput) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                ignoreTouchProgressUntilNextStart = false
                touchSeeking = true
                val session = seekSession
                when (session?.phase) {
                    SeekPhase.NonTouchPending -> {
                        cancelPendingNonTouchCommit()
                        session.phase = SeekPhase.Touch
                    }

                    SeekPhase.Touch -> Unit

                    SeekPhase.InFlight, null -> beginSeek(SeekPhase.Touch)
                }
            }

            override fun onProgressChanged(seekBar: SeekBar, progressSecs: Int, fromUser: Boolean) {
                if (!fromUser || ignoreTouchProgressUntilNextStart) return

                currentTime = progressSecs.coerceIn(0, seekBar.max).seconds
                updateTextViews()

                // TalkBack seek actions and keyboard arrows change the progress without touch
                // tracking callbacks, so debounce committing them or the next playback position
                // update would revert the bar.
                val session = if (touchSeeking) {
                    seekSession?.takeIf { it.phase == SeekPhase.Touch } ?: beginSeek(SeekPhase.Touch)
                } else {
                    seekSession?.takeIf { it.phase == SeekPhase.NonTouchPending }
                        ?: beginSeek(SeekPhase.NonTouchPending)
                }
                session.target = currentTime
                session.maxAtInput = duration.coerceAtLeast(Duration.ZERO)
                session.listener?.onSeekPositionChanging(currentTime)

                if (session.phase == SeekPhase.NonTouchPending) {
                    scheduleNonTouchCommit(session)
                }
            }
        })
    }

    private fun beginSeek(phase: SeekPhase): SeekSession {
        cancelPendingNonTouchCommit()
        val session = SeekSession(
            id = ++nextSeekSessionId,
            episodeUuid = episodeUuid,
            listener = changeListener,
            phase = phase,
            target = currentTime,
            maxAtInput = duration.coerceAtLeast(Duration.ZERO),
        )
        seekSession = session
        session.listener?.onSeekPositionChangeStart()
        return session
    }

    private fun scheduleNonTouchCommit(session: SeekSession) {
        cancelPendingNonTouchCommit()
        val target = session.target
        val maxAtInput = session.maxAtInput
        pendingNonTouchCommit = Runnable {
            commitSeek(session.id, target, maxAtInput)
        }.also { runnable ->
            postDelayed(runnable, NON_TOUCH_SEEK_COMMIT_DELAY_MS)
        }
    }

    private fun commitSeek(sessionId: Long, target: Duration, maxAtInput: Duration) {
        val session = seekSession?.takeIf {
            it.id == sessionId && it.phase != SeekPhase.InFlight
        } ?: return
        cancelPendingNonTouchCommit()
        session.phase = SeekPhase.InFlight
        val max = if (session.episodeUuid == episodeUuid) {
            duration.coerceAtLeast(Duration.ZERO)
        } else {
            maxAtInput
        }
        val boundedTarget = target.coerceIn(Duration.ZERO, max)
        val listener = session.listener
        if (listener == null) {
            finishSeek(sessionId)
        } else {
            listener.onSeekPositionChangeStop(session.episodeUuid, boundedTarget) {
                post { finishSeek(sessionId) }
            }
        }
    }

    private fun finishSeek(sessionId: Long) {
        val session = seekSession
        if (session?.id == sessionId && session.phase == SeekPhase.InFlight) {
            seekSession = null
        }
    }

    private fun cancelPendingNonTouchCommit() {
        pendingNonTouchCommit?.let(::removeCallbacks)
        pendingNonTouchCommit = null
    }

    private fun finishSeekForLifecycleChange() {
        val session = seekSession
        when (session?.phase) {
            SeekPhase.NonTouchPending -> commitSeek(session.id, session.target, session.maxAtInput)

            SeekPhase.Touch -> {
                ignoreTouchProgressUntilNextStart = true
            }

            SeekPhase.InFlight, null -> Unit
        }
        session?.listener?.onSeekPositionChangeCancel()
        cancelPendingNonTouchCommit()
        seekSession = null
        touchSeeking = false
    }

    override fun onDetachedFromWindow() {
        // Accessibility and keyboard progress changes are complete user actions even though their
        // player seek is debounced, so flush them before this view goes away. Active touch gestures
        // are cancelled instead.
        finishSeekForLifecycleChange()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // A touch gesture cannot continue across a detach/attach boundary. Episode changes still
        // keep this flag set until the original gesture ends, but a reused view must immediately
        // accept TalkBack and keyboard actions again.
        ignoreTouchProgressUntilNextStart = false
    }

    private fun updateTextViews() {
        if (duration <= Duration.ZERO) {
            elapsedTimeText.text = ""
            elapsedTimeText.contentDescription = ""
            remainingTimeText.text = ""
            remainingTimeText.contentDescription = ""
        } else {
            val elapsedTime = currentTime.toHhMmSs()
            elapsedTimeText.text = currentTime.toHhMmSs()
            elapsedTimeText.contentDescription = resources.getString(LR.string.player_played_up_to, elapsedTime)
            val timeLeft = remainingDuration()
            val remainingTime = buildString {
                if (timeLeft > Duration.ZERO) {
                    append('-')
                }
                append(timeLeft.toHhMmSs())
            }
            remainingTimeText.text = remainingTime
            remainingTimeText.contentDescription = resources.getString(LR.string.player_time_remaining, remainingTime.removePrefix("-"))
        }
    }

    private fun remainingDuration(): Duration {
        return if (adjustDuration) {
            (duration - currentTime - chapters.skippedChaptersDuration(currentTime)) / playbackSpeed
        } else {
            duration - currentTime
        }.coerceAtLeast(Duration.ZERO)
    }

    interface OnUserSeekListener {
        fun onSeekPositionChangeStop(episodeUuid: String, progress: Duration, seekComplete: () -> Unit)
        fun onSeekPositionChanging(progress: Duration)
        fun onSeekPositionChangeStart()
        fun onSeekPositionChangeCancel()
    }

    private data class SeekSession(
        val id: Long,
        val episodeUuid: String,
        val listener: OnUserSeekListener?,
        var phase: SeekPhase,
        var target: Duration,
        var maxAtInput: Duration,
    )

    private enum class SeekPhase {
        NonTouchPending,
        Touch,
        InFlight,
    }

    private companion object {
        const val NON_TOUCH_SEEK_COMMIT_DELAY_MS = 750L
    }
}
