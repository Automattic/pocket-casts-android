package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.share.ShareListCreateActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PodcastsOptionsDialog(
    val fragment: Fragment,
    val settings: Settings
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
                click = { openSortOptions() }
            )
            .addToggleOptions(
                LR.string.podcasts_menu_layout,
                R.drawable.ic_largegrid,
                ToggleButtonOption(
                    imageId = R.drawable.ic_largegrid,
                    descriptionId = LR.string.podcasts_layout_large_grid,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.LARGE_ARTWORK.id },
                    click = { settings.setPodcastsLayout(Settings.PodcastGridLayoutType.LARGE_ARTWORK.id) }
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_smallgrid,
                    descriptionId = LR.string.podcasts_layout_small_grid,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.SMALL_ARTWORK.id },
                    click = { settings.setPodcastsLayout(Settings.PodcastGridLayoutType.SMALL_ARTWORK.id) }
                ),
                ToggleButtonOption(
                    imageId = R.drawable.ic_list,
                    descriptionId = LR.string.podcasts_layout_list_view,
                    isOn = { settings.getPodcastsLayout() == Settings.PodcastGridLayoutType.LIST_VIEW.id },
                    click = { settings.setPodcastsLayout(Settings.PodcastGridLayoutType.LIST_VIEW.id) }
                )
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_badges,
                imageId = R.drawable.ic_badge,
                valueId = settings.getPodcastBadgeType().labelId,
                click = { openBadgeOptions() }
            )
            .addTextOption(
                titleId = LR.string.podcasts_menu_share_podcasts,
                imageId = R.drawable.ic_share_option,
                click = { sharePodcasts() }
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
                click = { settings.setPodcastsSortType(sortType = order, sync = true) }
            )
        }
        fragmentManager?.let {
            dialog.show(it, "podcasts_sort_dialog")
            sortDialog = dialog
        }
    }

    private fun openBadgeOptions() {
        val badgeType: Settings.BadgeType = settings.getPodcastBadgeType()
        val title = fragment.getString(LR.string.podcasts_menu_badges)
        val dialog = OptionsDialog()
            .setTitle(title)
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_off,
                checked = badgeType == Settings.BadgeType.OFF,
                click = { settings.setPodcastBadgeType(Settings.BadgeType.OFF) }
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_all_unfinished,
                checked = badgeType == Settings.BadgeType.ALL_UNFINISHED,
                click = { settings.setPodcastBadgeType(Settings.BadgeType.ALL_UNFINISHED) }
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_badges_only_latest_episode,
                checked = badgeType == Settings.BadgeType.LATEST_EPISODE,
                click = { settings.setPodcastBadgeType(Settings.BadgeType.LATEST_EPISODE) }
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
}
