package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.utils.extensions.findParcelableList
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.DiscoverRegionChangedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val ARG_REGION_LIST = "regionlist"
private const val ARG_SELECTED_REGION = "selectedregion"

@AndroidEntryPoint
class RegionSelectFragment : BaseFragment() {
    interface Listener {
        fun onRegionSelected(region: DiscoverRegion)
    }

    companion object {
        private const val REGION_KEY = "region"
        fun newInstance(regionList: List<DiscoverRegion>, selectedRegion: DiscoverRegion): RegionSelectFragment {
            val regionArrayList: ArrayList<DiscoverRegion> = ArrayList()
            regionArrayList.addAll(regionList)

            val bundle = Bundle()
            bundle.apply {
                putParcelableArrayList(ARG_REGION_LIST, regionArrayList)
                putString(ARG_SELECTED_REGION, selectedRegion.code)
            }
            val fragment = RegionSelectFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    val regionList: ArrayList<DiscoverRegion>
        get() = arguments?.findParcelableList<DiscoverRegion>(ARG_REGION_LIST) ?: ArrayList()

    var listener: Listener? = null

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var settings: Settings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
        val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
        AppTheme(theme.activeTheme) {
            RegionSelectPage(
                regions = remember { regionList.sortedBy { it.name } },
                selectedRegionCode = remember { arguments?.getString(ARG_SELECTED_REGION) },
                bottomInset = PaddingValues(bottom = bottomInset),
                onRegionClick = ::onRegionClick,
                onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                toolbarModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }

    private fun onRegionClick(region: DiscoverRegion) {
        listener?.onRegionSelected(region)
        eventHorizon.track(
            DiscoverRegionChangedEvent(
                region = region.code,
            ),
        )
    }
}
