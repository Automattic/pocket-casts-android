package au.com.shiftyjelly.pocketcasts.player.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShelfBinding
import au.com.shiftyjelly.pocketcasts.player.view.shelf.MenuShelfItems
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.moreActionsTitle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.shortcutTitle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject
import kotlin.collections.filterIsInstance
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import timber.log.Timber
import androidx.compose.ui.graphics.Color as composeColor
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ShelfFragment : BaseFragment() {
    private var items = emptyList<ShelfRowItem>()

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var settings: Settings

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentShelfBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShelfBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val toolbar = binding.toolbar
        toolbar.setTitle(LR.string.rearrange_actions)
        toolbar.setTitleTextColor(toolbar.context.getThemeColor(UR.attr.player_contrast_01))
        toolbar.setNavigationOnClickListener {
            trackRearrangeFinishedEvent()
            (activity as? FragmentHostListener)?.closeModal(this)
        }
        toolbar.navigationIcon?.setTint(ThemeColor.playerContrast01(theme.activeTheme))

        val backgroundColor = theme.playerBackground2Color(playerViewModel.podcast)
        val toolbarColor = theme.playerBackgroundColor(playerViewModel.podcast)
        view.setBackgroundColor(backgroundColor)
        toolbar.setBackgroundColor(toolbarColor)

        setupShelfListView(
            onDragComplete = { fromIndex, toIndex, shelfItem ->
                trackShelfItemMovedEvent(fromIndex, toIndex, shelfItem)
                settings.shelfItems.set(items.filterIsInstance<ShelfItem>(), updateModifiedAt = true)
            },
        )
    }

    private fun setupShelfListView(
        onDragComplete: (Int, Int, ShelfItem) -> Unit,
    ) {
        binding?.shelfItemsComposeView?.setContent {
            AppTheme(theme.activeTheme) {
                val backgroundColor = theme.playerBackground2Color(playerViewModel.podcast)
                val selectedColor = theme.playerHighlight7Color(playerViewModel.podcast)
                val selectedBackground = ColorUtils.calculateCombinedColor(backgroundColor, selectedColor)

                val shelfItems: List<ShelfRowItem> by playerViewModel.shelfLive.asFlow()
                    .map {
                        buildList<ShelfRowItem> {
                            addAll(it)
                            add(4, moreActionsTitle)
                            add(0, shortcutTitle)
                        }
                    }
                    .collectAsStateWithLifecycle(emptyList<ShelfRowItem>())

                val episode by playerViewModel.playingEpisodeLive.asFlow()
                    .map { (episode, _) -> episode }
                    .distinctUntilChangedBy { it.uuid }
                    .collectAsStateWithLifecycle(null)
                if (episode == null) return@AppTheme

                MenuShelfItems(
                    shelfItems = shelfItems,
                    episode = episode as BaseEpisode,
                    normalBackgroundColor = composeColor(Color.parseColor(ColorUtils.colorIntToHexString(backgroundColor))),
                    selectedBackgroundColor = composeColor(Color.parseColor(ColorUtils.colorIntToHexString(selectedBackground))),
                    isEditable = true,
                    onShelfItemMove = { items, fromPosition, toPosition ->
                        onShelfItemMove(items, fromPosition, toPosition, onDragComplete)
                        this@ShelfFragment.items
                    },
                )
            }
        }
    }

    override fun onBackPressed(): Boolean {
        trackRearrangeFinishedEvent()
        return super.onBackPressed()
    }

    private fun onShelfItemMove(
        items: List<ShelfRowItem>,
        fromPosition: Int,
        toPosition: Int,
        onDragComplete: (Int, Int, ShelfItem) -> Unit,
    ) {
        val listData = items.toMutableList()

        Timber.d("Swapping $fromPosition to $toPosition")
        Timber.d("List: $listData")
        val selectedShelfItem = listData[fromPosition] as ShelfItem

        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(listData, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(listData, index, index - 1)
            }
        }

        // Make sure the titles are in the right spot
        listData.remove(moreActionsTitle)
        listData.remove(shortcutTitle)
        listData.add(4, moreActionsTitle)
        listData.add(0, shortcutTitle)

        this@ShelfFragment.items = listData.toList()
        onDragComplete(fromPosition, toPosition, selectedShelfItem)
        Timber.d("Swapped: $listData")
    }

    private fun sectionTitleAt(position: Int) =
        if (position < items.indexOf(moreActionsTitle)) AnalyticsProp.Value.SHELF else AnalyticsProp.Value.OVERFLOW_MENU

    private fun trackShelfItemMovedEvent(fromPosition: Int, toPosition: Int, shelfItem: ShelfItem) {
        val title = shelfItem.analyticsValue
        val movedFrom = sectionTitleAt(fromPosition)
        val movedTo = sectionTitleAt(toPosition)
        val newPosition = if (movedTo == AnalyticsProp.Value.SHELF) {
            toPosition - 1
        } else {
            toPosition - (items.indexOf(moreActionsTitle) + 1)
        }
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to title,
                AnalyticsProp.Key.POSITION to newPosition, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to movedFrom,
                AnalyticsProp.Key.MOVED_TO to movedTo,
            ),
        )
    }

    private fun trackRearrangeFinishedEvent() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_FINISHED)
    }

    companion object {
        object AnalyticsProp {
            object Key {
                const val FROM = "from"
                const val ACTION = "action"
                const val MOVED_FROM = "moved_from"
                const val MOVED_TO = "moved_to"
                const val POSITION = "position"
                const val SOURCE = "source"
                const val EPISODE_UUID = "episode_uuid"
            }
            object Value {
                const val SHELF = "shelf"
                const val OVERFLOW_MENU = "overflow_menu"
                const val UNKNOWN = "unknown"
            }
        }
    }
}
