package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.databinding.FragmentMultiselectBottomSheetBinding
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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
            instance.arguments = bundleOf(
                ARG_ACTION_IDS to itemIds.toIntArray(),
                ARG_SHOULD_SHOW_REMOVE_LISTENING_HISTORY to shouldShowRemoveListeningHistory,
            )
            return instance
        }
    }

    @Inject lateinit var castManager: CastManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    var multiSelectHelper: MultiSelectEpisodesHelper? = null

    private val adapter = MultiSelectAdapter(editable = false, listener = this::onClick, dragListener = null)
    private var binding: FragmentMultiselectBottomSheetBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMultiselectBottomSheetBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        if (multiSelectHelper == null) {
            dismiss()
            return
        }

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL))

        val items = arguments?.getIntArray(ARG_ACTION_IDS)?.map { MultiSelectEpisodeAction.ALL_BY_ACTION_ID[it] }?.toMutableList() ?: mutableListOf()
        val shouldShowRemoveListeningHistory = arguments?.getBoolean(ARG_SHOULD_SHOW_REMOVE_LISTENING_HISTORY) ?: false

        if (!shouldShowRemoveListeningHistory) {
            items.removeAll { it is MultiSelectEpisodeAction.RemoveListeningHistory }
        }

        adapter.submitList(items + listOf(MultiSelectAction.SelectAll))

        multiSelectHelper?.selectedCount?.observe(viewLifecycleOwner) {
            binding.lblTitle.text = view.resources.getStringPlural(count = it, singular = LR.string.podcast_selected_episode_singular, plural = LR.string.podcast_selected_episode_plural)
        }

        binding.btnEdit.setOnClickListener {
            lifecycleScope.launch {
                val selectedEpisodes: List<BaseEpisode> = multiSelectHelper?.selectedListLive
                    ?.asFlow()
                    ?.first() ?: emptyList()

                val hasPlayedAnyEpisode = selectedEpisodes.any { episode ->
                    episode is PodcastEpisode && (episode.lastPlaybackInteraction ?: 0L) > 0L
                }

                val source = (multiSelectHelper?.source ?: SourceView.UNKNOWN)
                analyticsTracker.track(
                    AnalyticsEvent.MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_STARTED,
                    AnalyticsProp.sourceMap(source),
                )
                (activity as FragmentHostListener).showModal(
                    MultiSelectFragment.newInstance(source, shouldShowRemoveListeningHistory = hasPlayedAnyEpisode && source == SourceView.LISTENING_HISTORY),
                )
                dismiss()
            }
        }
    }

    private fun onClick(item: MultiSelectAction) {
        multiSelectHelper?.onMenuItemSelected(item.actionId, resources, requireActivity())
        dismiss()
    }

    private object AnalyticsProp {
        private const val SOURCE = "source"

        fun sourceMap(eventSource: SourceView) = mapOf(SOURCE to eventSource.analyticsValue)
    }
}
