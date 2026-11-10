package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectRearrangePage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class MultiSelectFragment : BaseFragment() {
    @Inject lateinit var settings: Settings

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var multiSelectEpisodesHelper: MultiSelectEpisodesHelper

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_KEY)

    private val rearrangeState by lazy {
        MultiSelectRearrangeState(
            settings = settings,
            eventHorizon = eventHorizon,
            maxToolbarIcons = multiSelectEpisodesHelper.maxToolbarIcons,
            source = args.source,
            shouldShowRemoveListeningHistory = args.shouldShowRemoveListeningHistory,
        ).apply { onSettingsChanged(settings.multiSelectItemsFlow.value) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val groupIds by settings.multiSelectItemsFlow.collectAsStateWithLifecycle()
            LaunchedEffect(groupIds) { rearrangeState.onSettingsChanged(groupIds) }
            val items by rearrangeState.items.collectAsStateWithLifecycle()
            MultiSelectRearrangePage(
                items = items,
                onNavigationClick = {
                    rearrangeState.onFinished()
                    (activity as? FragmentHostListener)?.closeModal(this)
                },
                onToolbarLongClick = { (activity as? AppCompatActivity)?.let(theme::toggleDarkLightThemeActivity) },
                onItemMove = rearrangeState::onItemMove,
                onItemStartDrag = rearrangeState::onItemStartDrag,
                onItemDrop = rearrangeState::onItemTouchFinished,
            )
        }
    }

    override fun onBackPressed(): Boolean {
        rearrangeState.onFinished()
        return super.onBackPressed()
    }

    @Parcelize
    private class Args(
        val source: SourceView,
        val shouldShowRemoveListeningHistory: Boolean,
    ) : Parcelable

    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            source: SourceView,
            shouldShowRemoveListeningHistory: Boolean,
        ) = MultiSelectFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_KEY,
                    Args(source, shouldShowRemoveListeningHistory),
                )
            }
        }
    }
}
