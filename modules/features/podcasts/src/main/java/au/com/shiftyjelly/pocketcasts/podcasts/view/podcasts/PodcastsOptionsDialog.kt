package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.share.ShareListCreateActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastListModalOptionType
import com.automattic.eventhorizon.PodcastsListBadgesChangedEvent
import com.automattic.eventhorizon.PodcastsListLayoutChangedEvent
import com.automattic.eventhorizon.PodcastsListModalOptionTappedEvent
import com.automattic.eventhorizon.PodcastsListSortOrderChangedEvent
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PodcastsOptionsDialog(
    val fragment: Fragment,
    val settings: Settings,
    private val eventHorizon: EventHorizon,
) {
    private var showDialog: OptionsDialog? = null
    private var sortDialog: OptionsDialog? = null
    private var badgeDialog: OptionsDialog? = null
    private val fragmentManager: FragmentManager?
        get() = fragment.activity?.supportFragmentManager

    fun show() {
        val dialog = OptionsDialog()
            .addTextOption(
                titleId = LR.string.podcasts_menu_sort_by,
                imageId = IR.drawable.ic_sort,
                valueId = settings.podcastsSortType.value.labelId,
                click = {
                    openSortOptions()
                    trackTapOnModalOption(ModalOption.SORT_BY)
                },
            )
            .addToggleOptions(
                LR.string.podcasts_menu_layout,
                R.drawable.ic_largegrid,
                ToggleButtonOption(
                    imageId = R.drawable.ic_largegrid,
                    descriptionId = LR.string.podcasts_layout_large_grid,
                    isOn = { settings.podcastGridLayout.value == PodcastGridLayoutType.LARGE_ARTWORK },
                    click = {
                        settings.podcastGridLayout.set(PodcastGridLayoutType.LARGE_ARTWORK, updateModifiedAt = true)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(PodcastGridLayoutType.LARGE_ARTWORK)
                    },
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_smallgrid,
                    descriptionId = LR.string.podcasts_layout_small_grid,
                    isOn = { settings.podcastGridLayout.value == PodcastGridLayoutType.SMALL_ARTWORK },
                    click = {
                        settings.podcastGridLayout.set(PodcastGridLayoutType.SMALL_ARTWORK, updateModifiedAt = true)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(PodcastGridLayoutType.SMALL_ARTWORK)
                    },
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_list,
                    descriptionId = LR.string.podcasts_layout_list_view,
                    isOn = { settings.podcastGridLayout.value == PodcastGridLayoutType.LIST_VIEW },
                    click = {
                        settings.podcastGridLayout.set(PodcastGridLayoutType.LIST_VIEW, updateModifiedAt = true)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(PodcastGridLayoutType.LIST_VIEW)
                    },
                ),
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_badges,
                imageId = R.drawable.ic_badge,
                valueId = settings.podcastBadgeType.value.labelId,
                click = {
                    openBadgeOptions()
                    trackTapOnModalOption(ModalOption.BADGE)
                },
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_share_podcasts,
                imageId = R.drawable.ic_share_option,
                click = {
                    sharePodcasts()
                    trackTapOnModalOption(ModalOption.SHARE)
                },
            )
        fragmentManager?.let {
            dialog.show(it, "podcasts_options_dialog")
            showDialog = dialog
        }
    }

    private fun sharePodcasts() {
        val activity = fragment.activity ?: return
        activity.startActivity(Intent(activity, ShareListCreateActivity::class.java))
    }

    private fun openSortOptions() {
        val sortOrder = settings.podcastsSortType.value
        val title = fragment.getString(LR.string.sort_by)
        val dialog = OptionsDialog().setTitle(title)
        val sortOptions = if (FeatureFlag.isEnabled(Feature.PODCASTS_SORT_CHANGES)) {
            PodcastsSortType.entries
        } else {
            PodcastsSortType.entries.filterNot { it == PodcastsSortType.RECENTLY_PLAYED }
        }
        for (order in sortOptions) {
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.clientId == sortOrder.clientId,
                click = {
                    settings.podcastsSortType.set(order, updateModifiedAt = true)
                    trackSortByChanged(order)
                },
            )
        }
        fragmentManager?.let {
            dialog.show(it, "podcasts_sort_dialog")
            sortDialog = dialog
        }
    }

    private fun openBadgeOptions() {
        val badgeType: BadgeType = settings.podcastBadgeType.value
        val title = fragment.getString(LR.string.podcasts_menu_badges)
        val dialog = OptionsDialog()
            .setTitle(title)
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_off,
                checked = badgeType == BadgeType.OFF,
                click = {
                    val newBadgeType = BadgeType.OFF
                    settings.podcastBadgeType.set(newBadgeType, updateModifiedAt = true)
                    trackBadgeChanged(newBadgeType)
                },
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_all_unfinished,
                checked = badgeType == BadgeType.ALL_UNFINISHED,
                click = {
                    val newBadgeType = BadgeType.ALL_UNFINISHED
                    settings.podcastBadgeType.set(newBadgeType, updateModifiedAt = true)
                    trackBadgeChanged(newBadgeType)
                },
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_only_latest_episode,
                checked = badgeType == BadgeType.LATEST_EPISODE,
                click = {
                    val newBadgeType = BadgeType.LATEST_EPISODE
                    settings.podcastBadgeType.set(newBadgeType, updateModifiedAt = true)
                    trackBadgeChanged(newBadgeType)
                },
            )
        fragmentManager?.let {
            dialog.show(it, "podcasts_badges")
            badgeDialog = dialog
        }
    }

    fun dismiss() {
        showDialog?.dismiss()
        sortDialog?.dismiss()
        badgeDialog?.dismiss()
    }

    private fun trackTapOnModalOption(option: ModalOption) {
        eventHorizon.track(
            PodcastsListModalOptionTappedEvent(
                option = option.eventHorizonValue,
            ),
        )
    }

    private fun trackSortByChanged(order: PodcastsSortType) {
        eventHorizon.track(
            PodcastsListSortOrderChangedEvent(
                sortBy = order.eventHorizonValue,
            ),
        )
    }

    private fun trackLayoutChanged(layoutType: PodcastGridLayoutType) {
        eventHorizon.track(
            PodcastsListLayoutChangedEvent(
                layout = layoutType.eventHorizonValue,
            ),
        )
    }

    private fun trackBadgeChanged(badgeType: BadgeType) {
        eventHorizon.track(
            PodcastsListBadgesChangedEvent(
                badge = badgeType.eventHorizonValue,
            ),
        )
    }

    enum class ModalOption(
        val eventHorizonValue: PodcastListModalOptionType,
    ) {
        SORT_BY(
            eventHorizonValue = PodcastListModalOptionType.SortBy,
        ),
        LAYOUT(
            eventHorizonValue = PodcastListModalOptionType.Layout,
        ),
        BADGE(
            eventHorizonValue = PodcastListModalOptionType.Badge,
        ),
        SHARE(
            eventHorizonValue = PodcastListModalOptionType.Share,
        ),
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val SORT_BY_KEY = "sort_by"
        private const val LAYOUT_KEY = "layout"
        private const val TYPE_KEY = "type"
    }
}
