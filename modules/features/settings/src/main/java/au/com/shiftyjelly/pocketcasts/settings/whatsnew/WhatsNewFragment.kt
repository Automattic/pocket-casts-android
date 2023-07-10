package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.PlaybackSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class WhatsNewFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setBackgroundColor(Color.Transparent.toArgb())
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    val onClose: () -> Unit = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    }
                    WhatsNewComposable(
                        onGoToSettings = {
                            onClose()
                            goToPlaybackSettings()
                        },
                        onClose = onClose,
                    )
                }
            }
        }

    private fun goToPlaybackSettings() {
        val fragmentHostListener = activity as? FragmentHostListener
            ?: throw IllegalStateException("Activity must implement FragmentHostListener")
        val fragment = PlaybackSettingsFragment.newInstance(scrollToAutoPlay = true)
        fragmentHostListener.addFragment(fragment)
    }

    companion object {
        fun isWhatsNewNewerThan(versionCode: Int?): Boolean {
            return Settings.WHATS_NEW_VERSION_CODE > (versionCode ?: 0)
        }
    }
}

@Composable
private fun WhatsNewComposable(
    onGoToSettings: () -> Unit,
    onClose: () -> Unit,
) {

    var closing by remember { mutableStateOf(false) }
    val targetAlpha = if (closing) 0f else 0.66f
    val scrimAlpha: Float by animateFloatAsState(
        targetValue = targetAlpha,
        finishedListener = { onClose() }
    )

    val performClose = {
        closing = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = performClose,
            )
            .padding(all = 16.dp)
            .fillMaxSize()
    ) {
        Column(Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
            val gradientBrush = Brush.linearGradient(
                0f to MaterialTheme.theme.colors.primaryInteractive01,
                1f to MaterialTheme.theme.colors.primaryInteractive01Hover,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(gradientBrush)
                    .fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 43.dp)
                        .clip(shape = CircleShape)
                        .background(Color.White)
                        .size(95.dp),

                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(IR.drawable.whatsnew_autoplay),
                        contentDescription = null,
                        modifier = Modifier
                            .height(24.dp)
                            .brush(gradientBrush),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(all = 16.dp),
            ) {

                TextH20(
                    text = stringResource(LR.string.whats_new_autoplay_title),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextP40(
                    text = stringResource(LR.string.whats_new_autoplay_body),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText02,
                )

                Spacer(modifier = Modifier.height(16.dp))

                RowButton(
                    text = stringResource(LR.string.whats_new_autoplay_enable_button),
                    onClick = onGoToSettings,
                    includePadding = false,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextH40(
                    text = stringResource(LR.string.whats_new_autoplay_maybe_later_button),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                    modifier = Modifier.clickable { performClose() },
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
