package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentChangeDoneBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_CLOSE_PARENT = "close_parent"

@AndroidEntryPoint
class ChangeDoneFragment : BaseFragment() {
    companion object {
        fun newInstance(closeParent: Boolean = false): ChangeDoneFragment {
            return ChangeDoneFragment().apply {
                arguments = bundleOf(ARG_CLOSE_PARENT to closeParent)
            }
        }
    }

    private val viewModel: DoneViewModel by activityViewModels()
    private var binding: FragmentChangeDoneBinding? = null

    private val shouldCloseParent: Boolean
        get() = arguments?.getBoolean(ARG_CLOSE_PARENT) ?: false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChangeDoneBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        UiUtil.hideKeyboard(view)
        binding.progress.isVisible = false

        activity?.let { _ ->
            val title = viewModel.title.value
            val detail = viewModel.detail.value
            val imageRef = viewModel.imageRef.value
            displaySuccess(title, detail, imageRef)

            binding.btnClose.setOnClickListener {
                closeForm()
            }

            binding.btnDone.setOnClickListener {
                closeForm()
            }
        }
    }

    private fun closeForm() {
        activity?.onBackPressedDispatcher?.apply {
            onBackPressed()
            if (shouldCloseParent) {
                onBackPressed()
            }
        }
    }

    private fun displaySuccess(primaryText: String?, secondaryText: String?, imageRef: Int?) {
        val binding = binding ?: return

        if (imageRef != null) {
            binding.imgDone.setImageResource(imageRef)
        }
        binding.imgDone.isVisible = true

        binding.txtDonePrimary.text = primaryText
        binding.txtDoneSecondary.text = secondaryText

        binding.txtDonePrimary.isVisible = true
        binding.txtDoneSecondary.isVisible = true
        binding.btnDone.isVisible = true
    }

    override fun onBackPressed(): Boolean {
        viewModel.trackDismissed()
        return super.onBackPressed()
    }
}
