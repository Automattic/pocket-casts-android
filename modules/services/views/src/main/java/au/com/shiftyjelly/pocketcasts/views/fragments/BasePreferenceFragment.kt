package au.com.shiftyjelly.pocketcasts.views.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        progressBar = createProgressBar()
        root.addView(progressBar)
        return root
    }

    override fun onDestroyView() {
        progressBar = null
        super.onDestroyView()
    }

    fun showLoading() {
        listView.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
    }

    fun hideLoading() {
        listView.visibility = View.VISIBLE
        progressBar?.visibility = View.GONE
    }

    private fun createProgressBar(): ProgressBar {
        return ProgressBar(requireContext()).apply {
            isIndeterminate = true
            layoutParams = FrameLayout.LayoutParams(
                24.dpToPx(requireContext()),
                24.dpToPx(requireContext())
            ).apply {
                gravity = Gravity.CENTER
            }
        }
    }
}
