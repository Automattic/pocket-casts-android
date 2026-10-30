package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.viewmodels.PodcastSelectViewModel
import com.automattic.eventhorizon.SelectPodcastsSourceType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class PodcastSelectFragment : BaseFragment() {
    @Inject
    lateinit var settings: Settings

    companion object {
        private const val NEW_INSTANCE_ARG = "tintColor"

        fun createArgs(
            @ColorInt tintColor: Int? = null,
            showToolbar: Boolean = false,
            source: PodcastSelectFragmentSource,
        ) = Bundle().apply {
            putParcelable(
                NEW_INSTANCE_ARG,
                PodcastSelectFragmentArgs(
                    tintColor = tintColor,
                    showToolbar = showToolbar,
                    source = source,
                ),
            )
        }

        fun newInstance(
            @ColorInt tintColor: Int? = null,
            showToolbar: Boolean = false,
            source: PodcastSelectFragmentSource,
        ): PodcastSelectFragment = PodcastSelectFragment().apply {
            arguments = createArgs(
                tintColor = tintColor,
                showToolbar = showToolbar,
                source = source,
            )
        }
    }

    interface Listener {
        fun podcastSelectFragmentSelectionChanged(newSelection: List<String>)
        fun podcastSelectFragmentGetCurrentSelection(): List<String>
    }

    lateinit var listener: Listener
    private var source: PodcastSelectFragmentSource? = null
    private var userChanged = false
    private var selectedUuids by mutableStateOf(emptySet<String>())

    private val viewModel: PodcastSelectViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is Listener) {
            listener = parentFragment as Listener
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val args = requireArguments().requireParcelable<PodcastSelectFragmentArgs>(NEW_INSTANCE_ARG)
        source = args.source
        source?.let { viewModel.trackOnShown(it) }

        viewModel.loadSelectablePodcasts(listener.podcastSelectFragmentGetCurrentSelection())

        return contentWithoutConsumedInsets {
            val selectablePodcasts by viewModel.selectablePodcasts.collectAsStateWithLifecycle()
            val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
            LaunchedEffect(selectablePodcasts) {
                selectedUuids = selectablePodcasts.filter { it.selected }.map { it.podcast.uuid }.toSet()
            }
            val podcasts = remember(selectablePodcasts) { selectablePodcasts.map { it.podcast } }
            AppTheme(theme.activeTheme) {
                PodcastSelectPage(
                    podcasts = podcasts,
                    selectedUuids = selectedUuids,
                    selectedText = LocalResources.current.getStringPluralPodcastsSelected(selectedUuids.size),
                    tintColor = args.tintColor?.let(::Color),
                    showToolbar = args.showToolbar,
                    bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() },
                    onToggle = { podcast, isSelected ->
                        source?.let { viewModel.trackOnPodcastToggled(it, podcast.uuid, isSelected) }
                        updateSelection(podcasts, if (isSelected) selectedUuids + podcast.uuid else selectedUuids - podcast.uuid)
                    },
                    onSelectButtonClick = {
                        if (selectedUuids.size == podcasts.size) {
                            source?.let { viewModel.trackOnSelectNoneTapped(it) }
                            updateSelection(podcasts, emptySet())
                        } else {
                            source?.let { viewModel.trackOnSelectAllTapped(it) }
                            updateSelection(podcasts, podcasts.map { it.uuid }.toSet())
                        }
                    },
                    onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }

    private fun updateSelection(podcasts: List<Podcast>, newSelection: Set<String>) {
        selectedUuids = newSelection
        listener.podcastSelectFragmentSelectionChanged(podcasts.map { it.uuid }.filter { it in newSelection })
        userChanged = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        source?.let { viewModel.trackOnDismissed(it) }
        if (userChanged) {
            val podcastCount = selectedUuids.size
            when (source) {
                PodcastSelectFragmentSource.AUTO_ADD -> viewModel.trackAutoAddPodcastsChanged(podcastCount)
                PodcastSelectFragmentSource.NOTIFICATIONS -> viewModel.trackNotificationsChanged(podcastCount)
                PodcastSelectFragmentSource.FILTERS, null -> Unit
            }
        }
    }
}

data class SelectablePodcast(val podcast: Podcast, var selected: Boolean)

@Parcelize
private data class PodcastSelectFragmentArgs(
    val tintColor: Int?,
    val showToolbar: Boolean,
    val source: PodcastSelectFragmentSource,
) : Parcelable

enum class PodcastSelectFragmentSource(
    val analyticsValue: SelectPodcastsSourceType,
) {
    AUTO_ADD(
        analyticsValue = SelectPodcastsSourceType.AutoAdd,
    ),
    NOTIFICATIONS(
        analyticsValue = SelectPodcastsSourceType.Notifications,
    ),
    FILTERS(
        analyticsValue = SelectPodcastsSourceType.Filters,
    ),
}
