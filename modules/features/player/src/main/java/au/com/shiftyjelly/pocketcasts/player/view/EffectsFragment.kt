package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentEffectsBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EffectsFragment : BaseDialogFragment() {

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentEffectsBinding? = null

    override val statusBarColor: StatusBarColor? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEffectsBinding.inflate(inflater, container, false)

        updateUi()

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }
    }

    private fun updateUi() {
        val binding = binding ?: return

        binding.podcastToggleGroup.check(binding.btnAllPodcasts.id)
        replaceFragment(AllPodcastsEffectFragment())

        binding.podcastToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val fragment: Fragment? = when (checkedId) {
                    binding.btnAllPodcasts.id -> AllPodcastsEffectFragment()
                    binding.btnThisPodcast.id -> ThisPodcastEffectFragment()
                    else -> null
                }

                fragment?.let {
                    replaceFragment(it)
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding?.effectsContainer?.id ?: return, fragment)
            .commit()
    }
}
