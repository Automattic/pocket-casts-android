package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhatsNewFeedFragment : BaseFragment() {
    private val viewModel by viewModels<WhatsNewFeedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(themeType = theme.activeTheme) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val shownIds = state.items.map(WhatsNewFeedItem::id)
            LaunchedEffect(shownIds) {
                viewModel.onMessagesShown(shownIds)
            }
            val bottomInsetPx by viewModel.bottomInset.collectAsStateWithLifecycle()
            val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
            WhatsNewFeedPage(
                state = state,
                bottomInset = bottomInset,
                onBackPress = { activity?.onBackPressedDispatcher?.onBackPressed() },
                onMessageClick = viewModel::onMessageClick,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::retry,
            )
        }
    }
}
