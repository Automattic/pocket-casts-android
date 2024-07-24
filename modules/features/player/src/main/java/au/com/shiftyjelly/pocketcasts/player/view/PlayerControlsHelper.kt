package au.com.shiftyjelly.pocketcasts.player.view

import au.com.shiftyjelly.pocketcasts.player.databinding.PlayerControlsBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.views.helper.toCircle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath

fun PlayerControlsBinding.initPlayerControls(
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipForwardLongPress: () -> Unit,
    onPlayClicked: () -> Unit,
) {
    skipBack.setOnClickListener {
        onSkipBack()
        (it as LottieAnimationView).playAnimation()
    }
    skipForward.setOnClickListener {
        onSkipForward()
        (it as LottieAnimationView).playAnimation()
    }
    skipForward.setOnLongClickListener {
        onSkipForwardLongPress()
        (it as LottieAnimationView).playAnimation()
        true
    }
    largePlayButton.setOnPlayClicked {
        onPlayClicked()
    }
}

fun PlayerControlsBinding.updatePlayerControls(
    headerViewModel: PlayerViewModel.PlayerHeader,
    color: Int,
) {
    largePlayButton.setPlaying(isPlaying = headerViewModel.isPlaying, animate = true)
    largePlayButton.setCircleTintColor(color)
    skipBackText.setTextColor(color)
    jumpForwardText.setTextColor(color)
    skipBack.post { // this only works the second time it's called unless it's in a post
        skipBack.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { SimpleColorFilter(color) }
        skipForward.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { SimpleColorFilter(color) }
    }
    skipForward.toCircle(true)
    jumpForwardText.text = headerViewModel.skipForwardInSecs.toString()
    skipBack.toCircle(true)
    skipBackText.text = headerViewModel.skipBackwardInSecs.toString()
}
