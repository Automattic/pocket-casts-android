package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.share.ShareListCreateActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PodcastsOptionsDialog(
    val fragment: Fragment,
    val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper
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
                valueId = settings.getPodcastsSortType().labelId,
                click = {
                    openSortOptions()
                    trackTapOnModalOption(ModalOption.SORT_BY)
                }
            )
            .addToggleOptions(
                LR.string.podcasts_menu_layout,
                R.drawable.ic_largegrid,
                ToggleButtonOption(
                    imageId = R.drawable.ic_largegrid,
                    descriptionId = LR.string.podcasts_layout_large_grid,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.LARGE_ARTWORK.id },
                    click = {
                        settings.setPodcastsLayout(Settings.PodcastGridLayoutType.LARGE_ARTWORK.id)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(Settings.PodcastGridLayoutType.LARGE_ARTWORK)
                    }
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_smallgrid,
                    descriptionId = LR.string.podcasts_layout_small_grid,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.SMALL_ARTWORK.id },
                    click = {
                        settings.setPodcastsLayout(Settings.PodcastGridLayoutType.SMALL_ARTWORK.id)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(Settings.PodcastGridLayoutType.SMALL_ARTWORK)
                    }
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_list,
                    descriptionId = LR.string.podcasts_layout_list_view,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.LIST_VIEW.id },
                    click = {
                        settings.setPodcastsLayout(Settings.PodcastGridLayoutType.LIST_VIEW.id)
                        trackTapOnModalOption(ModalOption.LAYOUT)
                        trackLayoutChanged(Settings.PodcastGridLayoutType.LIST_VIEW)
                    }
                )
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_badges,
                imageId = R.drawable.ic_badge,
                valueId = settings.podcastBadgeType.flow.value.labelId,
                click = {
                    openBadgeOptions()
                    trackTapOnModalOption(ModalOption.BADGE)
                }
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_share_podcasts,
                imageId = R.drawable.ic_share_option,
                click = {
                    sharePodcasts()
                    trackTapOnModalOption(ModalOption.SHARE)
                }
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
        val sortOrder = settings.getPodcastsSortType()
        val title = fragment.getString(LR.string.sort_by)
        val dialog = OptionsDialog().setTitle(title)
        for (order in PodcastsSortType.values()) {
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.clientId == sortOrder.clientId,
                click = {
                    settings.setPodcastsSortType(sortType = order, sync = true)
                    trackSortByChanged(order)
                }
            )
        }
        fragmentManager?.let {
            dialog.show(it, "podcasts_sort_dialog")
            sortDialog = dialog
        }
    }

    private fun openBadgeOptions() {
        val badgeType: BadgeType = settings.podcastBadgeType.flow.value
        val title = fragment.getString(LR.string.podcasts_menu_badges)
        val dialog = OptionsDialog()
            .setTitle(title)
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_off,
                checked = badgeType == BadgeType.OFF,
                click = {
                    val newBadgeType = BadgeType.OFF
                    settings.podcastBadgeType.set(newBadgeType)
                    trackBadgeChanged(newBadgeType)
                }
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_all_unfinished,
                checked = badgeType == BadgeType.ALL_UNFINISHED,
                click = {
                    val newBadgeType = BadgeType.ALL_UNFINISHED
                    settings.podcastBadgeType.set(newBadgeType)
                    trackBadgeChanged(newBadgeType)
                }
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_only_latest_episode,
                checked = badgeType == BadgeType.LATEST_EPISODE,
                click = {
                    val newBadgeType = BadgeType.LATEST_EPISODE
                    settings.podcastBadgeType.set(newBadgeType)
                    trackBadgeChanged(newBadgeType)
                }
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
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to option.analyticsValue))
    }

    private fun trackSortByChanged(order: PodcastsSortType) {
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_SORT_ORDER_CHANGED, mapOf(SORT_BY_KEY to order.analyticsValue))
    }

    private fun trackLayoutChanged(layoutType: Settings.PodcastGridLayoutType) {
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_LAYOUT_CHANGED, mapOf(LAYOUT_KEY to layoutType.analyticsValue))
    }

    private fun trackBadgeChanged(badgeType: BadgeType) {
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_BADGES_CHANGED, mapOf(TYPE_KEY to badgeType.analyticsValue))
    }

    enum class ModalOption(val analyticsValue: String) {
        SORT_BY("sort_by"),
        LAYOUT("layout"),
        BADGE("badge"),
        SHARE("share"),
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val SORT_BY_KEY = "sort_by"
        private const val LAYOUT_KEY = "layout"
        private const val TYPE_KEY = "type"
    }
}
