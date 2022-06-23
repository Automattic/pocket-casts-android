package au.com.shiftyjelly.pocketcasts.views.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator

object AnimatorUtil {

    fun fadeInAndSlideDown(view: View, durationMs: Int): Animator {
        val moveAnimator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_Y, -40f, 0f)
        moveAnimator.interpolator = AccelerateDecelerateInterpolator()
        moveAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val alphaAnimator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, 0f, 1f)
        alphaAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveAnimator, alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.visibility = View.VISIBLE
            }
        })

        return animatorSet
    }

    fun fadeOutAndSlideUp(view: View, durationMs: Int): Animator {
        val moveAnimator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_Y, 0f, -40f)
        moveAnimator.interpolator = AccelerateDecelerateInterpolator()
        moveAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val alphaAnimator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, 1f, 0f)
        alphaAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveAnimator, alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }
        })

        return animatorSet
    }

    fun fadeOutAndSlide(view: View, durationMs: Int, slideRight: Boolean): Animator {
        val moveAnimator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_X, 0f, if (slideRight) 40f else -40f)
        moveAnimator.interpolator = AccelerateDecelerateInterpolator()
        moveAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val alphaAnimator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, 1f, 0f)
        alphaAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveAnimator, alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                view.alpha = 1f
                view.translationX = 0f
            }
        })

        return animatorSet
    }

    fun fadeInAndSlide(view: View, durationMs: Int, slideRight: Boolean): Animator {
        val moveAnimator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_X, if (slideRight) -40f else 40f, 0f)
        moveAnimator.interpolator = AccelerateDecelerateInterpolator()
        moveAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val alphaAnimator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, 0f, 1f)
        alphaAnimator.duration = (if (durationMs < 0) 0 else durationMs).toLong()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveAnimator, alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.visibility = View.VISIBLE
            }
        })

        return animatorSet
    }

    fun fadeOut(view: View, durationMs: Int): ObjectAnimator {
        return fadeOut(view, 0f, durationMs)
    }

    fun fadeOut(view: View, toAlpha: Float, durationMs: Int): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, 1f, toAlpha)
        animator.duration = (if (durationMs < 0) 0 else durationMs).toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.visibility = View.VISIBLE
            }
        })
        return animator
    }

    fun fadeIn(view: View, durationMs: Int): ObjectAnimator {
        return fadeIn(view, 0f, durationMs)
    }

    fun fadeIn(view: View, fromAlpha: Float, durationMs: Int): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat<View>(view, View.ALPHA, fromAlpha, 1f)
        animator.duration = (if (durationMs < 0) 0 else durationMs).toLong()
        return animator
    }

    fun translationY(view: View, fromY: Int, toY: Int, interpolator: Interpolator, durationMs: Int): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_Y, fromY.toFloat(), toY.toFloat())
        animator.interpolator = interpolator
        animator.duration = (if (durationMs < 0) 0 else durationMs).toLong()
        return animator
    }

    fun translationX(view: View, fromY: Int, toY: Int, interpolator: Interpolator, durationMs: Int): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat<View>(view, View.TRANSLATION_X, fromY.toFloat(), toY.toFloat())
        animator.interpolator = interpolator
        animator.duration = (if (durationMs < 0) 0 else durationMs).toLong()
        return animator
    }

    fun zoomIn(view: View, duration: Int): Animator {
        val scale = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        )
        view.scaleX = 0f
        view.scaleY = 0f
        scale.duration = (if (duration < 0) 0 else duration).toLong()
        return scale
    }

    fun zoomOut(view: View, duration: Int): Animator {
        val scale = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0f),
            PropertyValuesHolder.ofFloat("scaleY", 0f)
        )
        view.scaleX = 1f
        view.scaleY = 1f
        scale.duration = (if (duration < 0) 0 else duration).toLong()
        scale.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                view.scaleY = 1f
                view.scaleX = 1f
            }
        })
        return scale
    }
}
