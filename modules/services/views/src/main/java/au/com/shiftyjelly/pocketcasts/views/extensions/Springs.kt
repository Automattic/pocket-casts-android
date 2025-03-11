package au.com.shiftyjelly.pocketcasts.views.extensions

import android.view.View
import androidx.annotation.IdRes
import androidx.dynamicanimation.animation.DynamicAnimation.ViewProperty
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import au.com.shiftyjelly.pocketcasts.views.R

fun View.spring(
    property: ViewProperty,
    damping: Float = 1f,
    stiffness: Float = 350f,
): SpringAnimation {
    val tagId = getTagId(property)
    var animation = getTag(tagId) as? SpringAnimation?
    if (animation == null) {
        animation = SpringAnimation(this, property).apply {
            spring = SpringForce().apply {
                this.dampingRatio = damping
                this.stiffness = stiffness
            }
        }
        setTag(tagId, animation)
    }
    return animation
}

@IdRes
private fun getTagId(property: ViewProperty) = when (property) {
    SpringAnimation.TRANSLATION_X -> R.id.spring_animation_id_translation_x
    SpringAnimation.TRANSLATION_Y -> R.id.spring_animation_id_translation_y
    SpringAnimation.TRANSLATION_Z -> R.id.spring_animation_id_translation_z
    SpringAnimation.SCALE_X -> R.id.spring_animation_id_scale_x
    SpringAnimation.SCALE_Y -> R.id.spring_animation_id_scale_y
    SpringAnimation.ROTATION -> R.id.spring_animation_id_rotation
    SpringAnimation.ROTATION_X -> R.id.spring_animation_id_rotation_x
    SpringAnimation.ROTATION_Y -> R.id.spring_animation_id_rotation_y
    SpringAnimation.X -> R.id.spring_animation_id_x
    SpringAnimation.Y -> R.id.spring_animation_id_y
    SpringAnimation.Z -> R.id.spring_animation_id_z
    SpringAnimation.ALPHA -> R.id.spring_animation_id_alpha
    SpringAnimation.SCROLL_X -> R.id.spring_animation_id_scroll_x
    SpringAnimation.SCROLL_Y -> R.id.spring_animation_id_scroll_y
    else -> error("Unknown view property: $property")
}

