package au.com.shiftyjelly.pocketcasts.views.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import au.com.shiftyjelly.pocketcasts.views.R
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath

private const val FRAME_MIN = 0
private const val FRAME_MAX = 19
private const val FRAME_PAUSE_IMAGE = 0
private const val FRAME_PLAY_IMAGE = 10
private const val FRAME_PLAY_IMAGE_ANIMATION = 9

class AnimatedPlayButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private var playing = false

    private val view = LayoutInflater.from(context).inflate(R.layout.animated_play_button, this, true)
    private val circleView = view.findViewById<View>(R.id.circleView)
    private val iconView = view.findViewById<LottieAnimationView>(R.id.iconView)
    private var width = 0f
    private var height = 0f

    init {
        circleView.clipToOutline = true
        circleView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        setPlaying(false, animate = true)
        updatePlayButtonFrame(isPlaying = false)

        // apply the icon width and height
        context.theme.obtainStyledAttributes(attrs, R.styleable.AnimatedPlayButton, 0, 0).apply {
            try {
                val iconTint = getInt(R.styleable.AnimatedPlayButton_icon_tint, 0)
                width = getDimension(R.styleable.AnimatedPlayButton_icon_width, 0f)
                height = getDimension(R.styleable.AnimatedPlayButton_icon_height, 0f)
                if (width != 0f && height != 0f) {
                    iconView.layoutParams.width = width.toInt()
                    iconView.layoutParams.height = height.toInt()
                }
                iconView.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { SimpleColorFilter(iconTint) }
            } finally {
                recycle()
            }
        }

        circleView.contentDescription = "Play button"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // if the dimensions aren't in the XML then size the icon relative to the layout
        if (width == 0f || height == 0f) {
            val mWidth = measuredWidth
            iconView.updateLayoutParams {
                width = (mWidth.toFloat() * .48f).toInt()
                height = width
            }
        }
    }

    fun toggleAnimated() {
        setPlaying(!playing, true)
    }

    fun setCircleTintColor(tintColor: Int) {
        ViewCompat.setBackgroundTintList(circleView, ColorStateList.valueOf(tintColor))
    }

    fun setPlaying(isPlaying: Boolean, animate: Boolean) {
        if (playing == isPlaying) {
            if ((iconView.drawable as? LottieDrawable)?.isAnimating == false) {
                updatePlayButtonFrame(isPlaying)
            }
            return
        }

        playing = isPlaying
        if (animate) {
            animatePlayButton(playing)
        } else {
            updatePlayButtonFrame(isPlaying)
        }

        if (isPlaying) {
            circleView.contentDescription = "Pause button"
        } else {
            circleView.contentDescription = "Play button"
        }
    }

    private fun animatePlayButton(isPlaying: Boolean) {
        val drawable = iconView.drawable
        if (drawable is LottieDrawable) {
            if (isPlaying) {
                // animate from the play to pause image
                drawable.setMinAndMaxFrame(FRAME_PLAY_IMAGE, FRAME_MAX)
            } else {
                // animate from the pause to play image (stop a frame early as under the hood it uses a float, goes over and doesn't finish straight)
                drawable.setMinAndMaxFrame(FRAME_MIN, FRAME_PLAY_IMAGE_ANIMATION)
            }
            drawable.playAnimation()
        }
    }

    private fun updatePlayButtonFrame(isPlaying: Boolean) {
        iconView.setMinAndMaxFrame(FRAME_MIN, FRAME_MAX)
        iconView.frame = if (isPlaying) FRAME_PAUSE_IMAGE else FRAME_PLAY_IMAGE
    }

    fun setOnPlayClicked(clicked: () -> Unit) {
        circleView.setOnClickListener { clicked() }
    }
}
