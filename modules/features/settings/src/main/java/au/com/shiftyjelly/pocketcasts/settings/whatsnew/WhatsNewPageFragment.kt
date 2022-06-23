package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentWhatsnewPageBinding
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNew
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val ARG_PAGE_INDEX = "argPageIndex"

class WhatsNewPageFragment : BaseFragment() {
    companion object {
        fun newInstance(pageIndex: Int): WhatsNewPageFragment {
            return WhatsNewPageFragment().apply {
                arguments = bundleOf(
                    ARG_PAGE_INDEX to pageIndex
                )
            }
        }
    }

    private var binding: FragmentWhatsnewPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWhatsnewPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding?.contentRecyclerView?.layoutManager = layoutManager

        val pages = WhatsNew.pages
        val pageIndex = arguments?.getInt(ARG_PAGE_INDEX)
        if (pageIndex != null && pageIndex < pages.count()) {
            val whatsNewPage = pages[pageIndex]
            binding?.contentRecyclerView?.adapter = WhatsNewAdapter(whatsNewPage) { whatsNewItem ->
                WebViewActivity.show(context, getString(LR.string.learn_more), whatsNewItem.url)
            }
        }
    }
}
