package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentRegionSelectBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowRegionBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_REGION_LIST = "regionlist"
private const val ARG_SELECTED_REGION = "selectedregion"

@AndroidEntryPoint
class RegionSelectFragment : BaseFragment() {
    interface Listener {
        fun onRegionSelected(region: DiscoverRegion)
    }

    companion object {
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
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_REGION_LIST, DiscoverRegion::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList(ARG_REGION_LIST)
        } ?: ArrayList()

    var listener: Listener? = null

    private var binding: FragmentRegionSelectBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRegionSelectBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.toolbar.setup(title = getString(LR.string.discover_select_a_region), navigationIcon = BackArrow, activity = activity, theme = theme)

        binding.recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val adapter = RegionAdapter(regionList.sortedBy { it.name }) {
            listener?.onRegionSelected(it)
        }
        adapter.selectedRegionCode = arguments?.getString(ARG_SELECTED_REGION)

        binding.recyclerView.adapter = adapter
    }
}

private class RegionAdapter(val regionList: List<DiscoverRegion>, val onRegionClicked: (DiscoverRegion) -> Unit) : RecyclerView.Adapter<RegionAdapter.RegionViewHolder>() {

    class RegionViewHolder(val binding: RowRegionBinding) : RecyclerView.ViewHolder(binding.root)

    var selectedRegionCode: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowRegionBinding.inflate(inflater, parent, false)
        return RegionViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return regionList.size
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        val region = regionList[position]
        holder.binding.imageView.load(region.flag) {
            allowHardware(false)
        }
        holder.binding.lblTitle.text = region.name.tryToLocalise(holder.itemView.resources)
        holder.itemView.setOnClickListener { onRegionClicked(region) }
        holder.binding.imgSelected.isVisible = region.code == selectedRegionCode
    }
}
