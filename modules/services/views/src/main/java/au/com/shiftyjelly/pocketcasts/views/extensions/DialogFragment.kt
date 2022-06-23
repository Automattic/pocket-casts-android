package au.com.shiftyjelly.pocketcasts.views.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

fun DialogFragment.showAllowingStateLoss(fragmentManager: FragmentManager, tag: String) {
    fragmentManager.beginTransaction().add(this, tag).commitAllowingStateLoss()
}
