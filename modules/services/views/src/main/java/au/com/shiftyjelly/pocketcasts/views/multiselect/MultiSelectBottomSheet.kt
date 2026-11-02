package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectBottomSheetPage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeStartedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_ACTION_IDS = "actionids"
private const val ARG_SHOULD_SHOW_REMOVE_LISTENING_HISTORY = "arg_should_show_remove_listening_history"

@AndroidEntryPoint
class MultiSelectBottomSheet : BaseDialogFragment() {
    companion object {
        fun newInstance(itemIds: List<Int>, shouldShowRemoveListeningHistory: Boolean): MultiSelectBottomSheet {
            val instance = MultiSelectBottomSheet()
            instance.arguments = Bundle().apply {
                putIntArray(ARG_ACTION_IDS, itemIds.toIntArray())
                putBoolean(
                    ARG_SHOULD_SHOW_REMOVE_LISTENING_HISTORY,
                    shouldShowRemoveListeningHistory,
                )
            }
            return instance
        }
    }

    @Inject lateinit var eventHorizon: EventHorizon

    var multiSelectHelper: MultiSelectEpisodesHelper? = null

    override val includeNavigationBarPadding = false

    private val shouldShowRemoveListeningHistory get() = arguments?.getBoolean(ARG_SHOULD_SHOW_REMOVE_LISTENING_HISTORY) ?: false

    private val actions: List<MultiSelectAction>
        get() {
            val items = arguments?.getIntArray(ARG_ACTION_IDS)?.map { MultiSelectEpisodeAction.ALL_BY_ACTION_ID.getValue(it) }.orEmpty()
            val visibleItems = if (shouldShowRemoveListeningHistory) items else items.filterNot { it is MultiSelectEpisodeAction.RemoveListeningHistory }
            return visibleItems + MultiSelectAction.SelectAll
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val helper = multiSelectHelper
            val selectedCountFlow = remember(helper) { helper?.selectedCount?.asFlow() ?: emptyFlow() }
            val selectedCount by selectedCountFlow.collectAsStateWithLifecycle(initialValue = helper?.selectedListLive?.value?.size)
            val resources = LocalResources.current
            MultiSelectBottomSheetPage(
                title = selectedCount?.let { resources.getStringPlural(count = it, singular = LR.string.podcast_selected_episode_singular, plural = LR.string.podcast_selected_episode_plural) }.orEmpty(),
                actions = remember { actions },
                onEditClick = ::onEditClick,
                onActionClick = ::onClick,
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)),
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (multiSelectHelper == null) {
            dismiss()
        }
    }

    private fun onEditClick() {
        lifecycleScope.launch {
            val selectedEpisodes: List<BaseEpisode> = multiSelectHelper?.selectedListLive
                ?.asFlow()
                ?.first() ?: emptyList()

            val hasPlayedAnyEpisode = selectedEpisodes.any { episode ->
                episode is PodcastEpisode && (episode.lastPlaybackInteraction ?: 0L) > 0L
            }

            val source = (multiSelectHelper?.source ?: SourceView.UNKNOWN)

            eventHorizon.track(
                MultiSelectViewOverflowMenuRearrangeStartedEvent(
                    source = source.analyticsValue,
                ),
            )
            (activity as FragmentHostListener).showModal(
                MultiSelectFragment.newInstance(source, shouldShowRemoveListeningHistory = hasPlayedAnyEpisode && source == SourceView.LISTENING_HISTORY),
            )
            dismiss()
        }
    }

    private fun onClick(item: MultiSelectAction) {
        multiSelectHelper?.onMenuItemSelected(item.actionId, resources, requireActivity())
        dismiss()
    }
}
