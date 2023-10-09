package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment

class GiveRatingFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            GiveRatingListenMore()
        }
    }
}
