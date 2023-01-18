package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FragmentCreateFilterBinding
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableIndex
import au.com.shiftyjelly.pocketcasts.repositories.extensions.iconDrawables
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColors
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themeColors
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.adapter.ColorAdapter
import au.com.shiftyjelly.pocketcasts.views.adapter.IconView
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.addAfterTextChanged
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val ARG_MODE = "mode"
private const val ARG_PLAYLIST_UUID = "playlist_uuid"

@AndroidEntryPoint
class CreateFilterFragment : BaseFragment(), CoroutineScope {
    sealed class Mode(val string: String) {
        object Create : Mode("create")
        data class Edit(val playlist: Playlist) : Mode("edit")
    }

    private var rootView: View? = null
    private var binding: FragmentCreateFilterBinding? = null

    companion object {
        fun newInstance(mode: Mode): CreateFilterFragment {
            return CreateFilterFragment().apply {
                arguments = bundleOf(
                    ARG_MODE to mode.string, ARG_PLAYLIST_UUID to (mode as? Mode.Edit)?.playlist?.uuid
                )
            }
        }
    }

    override var statusBarColor: StatusBarColor = StatusBarColor.Light
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    val viewModel by activityViewModels<CreateFilterViewModel>()

    val isCreate: Boolean
        get() = arguments?.getString(ARG_MODE) == "create"
    val playlistUUID: String?
        get() = arguments?.getString(ARG_PLAYLIST_UUID)

    private val iconViews: MutableList<IconView> = mutableListOf()
    private var selectedIconIndex: Int = 0
        set(value) {
            field = value
            viewModel.iconId = value
            if (selectedIconIndexInitialized) {
                viewModel.userChangedIcon()
            }
        }

    var tintColor: Int = 0
        set(value) {
            field = value
            updateIconViews()
        }

    private var txtNameInitialized = false
    private var selectedIconIndexInitialized = false

    override fun onPause() {
        super.onPause()
        rootView?.let {
            UiUtil.hideKeyboard(it)
        }
        if (!isCreate) {
            launch {
                viewModel.saveFilter(
                    iconIndex = selectedIconIndex,
                    colorIndex = colorAdapter.selectedIndex,
                    isCreatingNewFilter = isCreate
                )
                viewModel.reset()
            }
        }
        viewModel.isAutoDownloadSwitchInitialized = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateFilterBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private lateinit var colorAdapter: ColorAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isCreate) {
            runBlocking { viewModel.setup(playlistUUID) }
        }

        val colors = Playlist.getColors(context)
        colorAdapter = ColorAdapter(colors.toIntArray(), false) { index, fromUserInteraction ->
            tintColor = context?.getThemeColor(Playlist.themeColors[index]) ?: Color.WHITE
            viewModel.colorIndex.value = index
            if (fromUserInteraction) {
                viewModel.userChangedColor()
            }
        }
        tintColor = view.context.getThemeColor(Playlist.themeColors.first())

        setupIconViews()

        val binding = binding ?: return
        val recyclerColor = binding.recyclerColor

        val colorLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recyclerColor.layoutManager = colorLayoutManager
        recyclerColor.adapter = colorAdapter

        val txtName = binding.txtName
        val scrollView = binding.scrollView
        txtName.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.updatePadding(bottom = 300.dpToPx(txtName.context))
            } else {
                scrollView.updatePadding(bottom = 60.dpToPx(txtName.context))
                UiUtil.hideKeyboard(txtName)
            }
        }

        txtName.addAfterTextChanged {
            viewModel.filterName.value = it
            if (txtNameInitialized) {
                viewModel.userChangedFilterName()
            }
        }

        viewModel.playlist.observe(viewLifecycleOwner) { filter ->
            if (filter.title.isEmpty()) {
                txtName.setHint(LR.string.filters_filter_name)
            } else {
                txtNameInitialized = false
                txtName.setText(filter.title)
            }
            txtNameInitialized = true

            colorAdapter.setSelectedIndex(filter.colorIndex, fromUserInteraction = false)

            selectedIconIndexInitialized = false
            selectedIconIndex = filter.drawableIndex
            selectedIconIndexInitialized = true

            tintColor = filter.getColor(context)
            updateIconViews()

            binding.switchAutoDownload.isChecked = filter.autoDownload
            viewModel.isAutoDownloadSwitchInitialized = true

            binding.layoutDownloadLimit.isVisible = filter.autoDownload && !isCreate
            binding.lblDownloadLimit.text = getString(LR.string.filters_auto_download_limit, filter.autodownloadLimit)
        }

        viewModel.colorIndex.observe(viewLifecycleOwner) {
            val colorResId = Playlist.themeColors.getOrNull(it) ?: 0
            val context = view.context
            val tintColor = context.getThemeColor(colorResId)
            binding.nameInputLayout.boxStrokeColor = tintColor
            TextViewCompat.setCompoundDrawableTintList(txtName, ColorStateList.valueOf(tintColor))
        }

        if (isCreate) {
            binding.toolbarLayout.isVisible = false
        } else {
            binding.toolbar.setNavigationOnClickListener {
                @Suppress("DEPRECATION")
                (activity as AppCompatActivity).onBackPressed()
            }
        }

        val layoutAutoDownload = binding.layoutAutoDownload
        val layoutDownloadLimit = binding.layoutDownloadLimit
        val switchAutoDownload = binding.switchAutoDownload

        layoutAutoDownload.isVisible = !isCreate
        layoutDownloadLimit.isVisible = !isCreate

        layoutAutoDownload.setOnClickListener {
            switchAutoDownload.isChecked = !switchAutoDownload.isChecked
        }
        switchAutoDownload.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutodownload(isChecked)
            layoutDownloadLimit.isVisible = !isCreate && isChecked
        }

        layoutDownloadLimit.setOnClickListener {
            val dialog = OptionsDialog()
                .addTextOption(LR.string.filters_download_limit_5, click = { setDownloadLimit(5) })
                .addTextOption(LR.string.filters_download_limit_10, click = { setDownloadLimit(10) })
                .addTextOption(LR.string.filters_download_limit_20, click = { setDownloadLimit(20) })
                .addTextOption(LR.string.filters_download_limit_40, click = { setDownloadLimit(40) })
                .addTextOption(LR.string.filters_download_limit_100, click = { setDownloadLimit(100) })
                .setTitle(getString(LR.string.filters_download_title))
            dialog.show(parentFragmentManager, "auto_download_first")
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackPressed(isCreate)
        return super.onBackPressed()
    }

    private fun setupIconViews() {
        val context = context ?: return
        val binding = binding ?: return

        Playlist.iconDrawables.forEachIndexed { index, _ ->
            val imgRes = Playlist.iconDrawables[index]
            val view = IconView(context)
            val layoutParams = RecyclerView.LayoutParams(44.dpToPx(context), 44.dpToPx(context))
            layoutParams.marginEnd = 16.dpToPx(context)
            view.layoutParams = layoutParams

            view.setImageResource(imgRes)

            setupIconView(view, selectedIconIndex == index)
            view.setOnClickListener {
                setupIconView(iconViews[selectedIconIndex], false)
                selectedIconIndex = index
                setupIconView(view, true)

                UiUtil.hideKeyboard(binding.txtName)
            }

            binding.iconLayout.addView(view)
            iconViews.add(view)
        }
    }

    private fun updateIconViews() {
        iconViews.forEachIndexed { index, iconView ->
            setupIconView(iconView, index == selectedIconIndex)
        }

        val binding = binding ?: return
        val toolbar = binding.toolbar
        val titleColor = ThemeColor.filterText01(theme.activeTheme, tintColor)
        val iconColor = ThemeColor.filterIcon01(theme.activeTheme, tintColor)
        val iconColorStateList = ColorStateList.valueOf(iconColor)
        val backgroundColor = ThemeColor.filterUi01(theme.activeTheme, tintColor)
        toolbar.setTitleTextColor(titleColor)
        toolbar.navigationIcon?.setTintList(iconColorStateList)
        toolbar.setBackgroundColor(backgroundColor)
    }

    private fun setupIconView(view: IconView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(VR.drawable.filter_circle)
            view.imageTintList = ColorStateList.valueOf(view.context.getThemeColor(UR.attr.primary_interactive_02))
            view.backgroundTintList = ColorStateList.valueOf(tintColor)
            view.isSelected = true
        } else {
            view.setBackgroundResource(VR.drawable.filter_icon_button_unselected)
            view.imageTintList = ColorStateList.valueOf(ThemeColor.filterIcon02(theme.activeTheme, tintColor))
            view.backgroundTintList = null
        }
    }

    private fun setDownloadLimit(limit: Int) {
        binding?.lblDownloadLimit?.text = getString(LR.string.filters_auto_download_limit, limit)
        viewModel.updateDownloadLimit(limit)
    }
}
