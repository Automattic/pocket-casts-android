package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

abstract class LargeListAdapter<T : Any, VH : RecyclerView.ViewHolder>(private val animationCutoff: Int = 1500, differ: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(differ) {
    var lastListCount = 0
    private var nextDiffIsLarge: Boolean = false

    override fun submitList(list: List<T>?) {
        val newListCount = list?.count() ?: 0
        val diffSize = abs(lastListCount - newListCount)
        // If the list has changed size the diff happens faster, we only really want to skip
        // the diff when the list is the same size because it involves inspecting every item
        // to find changes.
        if (nextDiffIsLarge && diffSize == 0 && (lastListCount > animationCutoff || newListCount > animationCutoff)) {
            // HACK: When lists get too big the diff takes forever, we submit a null list
            // to effectively skip the diffing
            super.submitList(null)
            nextDiffIsLarge = false
        }
        super.submitList(list)

        lastListCount = newListCount
    }

    fun signalLargeDiff() {
        // This approach isn't great because it would be technically possible for
        // another update to come in in the mean time and reset this flag. We work around this
        // by only setting it back to false after the 2 lists are the same size (like a change of sort method).

        // We also don't just submit null lists from outside the adapter because we still want normal sized
        // podcasts to calculate their diff and animate
        nextDiffIsLarge = true
    }
}
