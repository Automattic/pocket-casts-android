package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentWhatsnewBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColors
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.Close
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNew
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNewPage
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class WhatsNewFragment : BaseFragment() {

    private var binding: FragmentWhatsnewBinding? = null

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val binding = binding ?: return
            binding.pageIndicatorView.position = position
            binding.btnNext.text = getString(if (position < WhatsNew.pages.count() - 1) LR.string.settings_whats_new_next else LR.string.settings_whats_new_done)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWhatsnewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.mainViewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.toolbar.let { toolbar ->
            toolbar.title = getString(LR.string.settings_whats_new)

            val colors = ToolbarColors.Theme(theme = theme, context = view.context)
            binding.toolbar.updateColors(toolbarColors = colors, navigationIcon = Close)
            updateStatusBarColor(color = colors.backgroundColor)

            toolbar.setNavigationOnClickListener {
                close()
            }
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
        }

        val fragmentAdapter = WhatsNewFragmentPagerAdapter(this)
        binding.mainViewPager.adapter = fragmentAdapter

        val pages = WhatsNew.pages
        binding.pageIndicatorView.count = pages.size
        binding.pageIndicatorView.isVisible = pages.size > 1

        binding.btnNext.text = getString(if (pages.count() > 1) LR.string.settings_whats_new_next else LR.string.settings_whats_new_done)
        (binding.mainViewPager.adapter as? WhatsNewFragmentPagerAdapter)?.updateItems(pages)

        binding.mainViewPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.btnNext.setOnClickListener {
            if (binding.mainViewPager.currentItem == pages.size - 1) {
                close()
            } else {
                binding.mainViewPager.setCurrentItem(binding.mainViewPager.currentItem + 1, true)
            }
        }
    }

    private fun close() {
        @Suppress("DEPRECATION")
        activity?.onBackPressed()
    }
}

class WhatsNewFragmentPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private var whatsNewPages = listOf<WhatsNewPage>()

    fun updateItems(newList: List<WhatsNewPage>) {
        whatsNewPages = newList
        notifyDataSetChanged()
    }

    override fun createFragment(position: Int): Fragment {
        return WhatsNewPageFragment.newInstance(position)
    }

    override fun getItemCount(): Int {
        return whatsNewPages.count()
    }
}
