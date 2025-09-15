package au.com.shiftyjelly.pocketcasts.views.swipe

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.databinding.SwipeButtonBinding
import android.R as AndroidR

class SwipeButton<T : SwipeButton.UiState> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding = SwipeButtonBinding.inflate(LayoutInflater.from(context), this)

    internal val outerContainer = binding.outerContainer

    internal val innerContainer = binding.innerContainer

    /**
     * This state prevents the UI from updating while a button action is being executed.
     * When an action is triggered, it may result in data change and swap to a different action.
     * For example, adding an episode to “Up Next” changes the action to “Remove from Up Next.”
     * To avoid confusing the user with sudden changes before the swipe state has settled,
     * we memoize the current state and keep it until the animation completes.
     *
     * The logic for this behavior is implemented in [SwipeRowLayout].
     */
    private var lockedUiState: T? = null

    private var currentUiState: T? = null

    internal var isLocked = false
        set(value) {
            field = value
            if (!value) {
                lockedUiState = currentUiState
            }
            applyUiState()
        }

    val uiState: T? get() = if (isLocked) lockedUiState else currentUiState

    val settledWidth get() = binding.swipeImage.width

    private val onSwipeActionListeners = mutableSetOf<(T) -> Unit>()

    init {
        clipChildren = false
        val imageGravity = context.theme
            .obtainStyledAttributes(attrs, R.styleable.SwipeButton, 0, 0)
            .use { array ->
                when (array.getInt(R.styleable.SwipeButton_imageGravity, 0)) {
                    0 -> ImageGravity.Left
                    1 -> ImageGravity.Right
                    else -> ImageGravity.Left
                }
            }
        binding.swipeImage.updateLayoutParams<LayoutParams> {
            gravity = gravity or imageGravity.androidValue
        }
        binding.innerContainer.setOnClickListener { callOnSwipeActionListeners() }
    }

    fun setUiState(uiState: T?) {
        if (currentUiState != uiState) {
            currentUiState = uiState
            if (!isLocked) {
                lockedUiState = uiState
            }
            applyUiState()
        }
    }

    fun addOnSwipeActionListener(onSwipeActionListener: (T) -> Unit) {
        onSwipeActionListeners.add(onSwipeActionListener)
    }

    fun removeOnSwipeActionListener(onSwipeActionListener: ((T) -> Unit)) {
        onSwipeActionListeners.remove(onSwipeActionListener)
    }

    internal fun callOnSwipeActionListeners() {
        uiState?.let { state -> onSwipeActionListeners.forEach { it(state) } }
    }

    private fun applyUiState() {
        isVisible = uiState != null
        applyContentDescription()
        applyBackgroundTint()
        applyImageDrawableId()
        applyImageTint()
    }

    private fun applyBackgroundTint() {
        val tint = uiState?.backgroundTint(context)
        if (tint != null) {
            innerContainer.setBackgroundColor(tint)
        } else {
            innerContainer.background = null
        }
    }

    private fun applyContentDescription() {
        val text = uiState?.contentDescription(context)
        innerContainer.contentDescription = text
    }

    private var appliedImageDrawableId: Int? = null

    private fun applyImageDrawableId() {
        val drawableId = uiState?.imageDrawableId()
        if (appliedImageDrawableId == drawableId) {
            return
        }
        appliedImageDrawableId = drawableId

        binding.swipeImage.setImageDrawable(drawableId?.let { AppCompatResources.getDrawable(context, it) })
    }

    private var appliedImageTint: Int? = null

    private fun applyImageTint() {
        val tint = uiState?.imageTint(context)
        if (appliedImageTint == tint) {
            return
        }
        appliedImageTint = tint

        val iconTintList = tint?.let(ColorStateList::valueOf)
        ImageViewCompat.setImageTintList(binding.swipeImage, iconTintList)

        innerContainer.foreground = if (tint != null) {
            val typedValue = TypedValue().apply {
                context.theme.resolveAttribute(AndroidR.attr.selectableItemBackground, this, true)
            }
            val drawable = AppCompatResources.getDrawable(context, typedValue.resourceId)
            if (drawable is RippleDrawable) {
                val rippleTintList = ColorStateList.valueOf(ColorUtils.colorWithAlpha(tint, 78))
                drawable.setColor(rippleTintList)
            }
            drawable
        } else {
            null
        }
    }

    interface UiState {
        fun contentDescription(context: Context): String

        @ColorInt
        fun backgroundTint(context: Context): Int

        @ColorInt
        fun imageTint(context: Context): Int

        @DrawableRes
        fun imageDrawableId(): Int
    }

    @SuppressLint("RtlHardcoded")
    private enum class ImageGravity(
        val androidValue: Int,
    ) {
        Left(androidValue = Gravity.LEFT),
        Right(androidValue = Gravity.RIGHT),
    }
}
