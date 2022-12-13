package au.com.shiftyjelly.pocketcasts.profile

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CancelConfirmationFragment : BaseDialogFragment() {
    companion object {
        fun newInstance(): CancelConfirmationFragment {
            return CancelConfirmationFragment()
        }
    }

    @Inject
    lateinit var settings: Settings
    private val viewModel: CancelConfirmationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                CancelConfirmationPage(
                    rows = getRows(),
                    onStayClicked = {
                        viewModel.onStayClicked()
                        closeScreen()
                    },
                    onCloseClicked = ::closeScreen,
                    onCancelClicked = ::onCancelClicked,
                )
            }
        }
    }

    private fun closeScreen() {
        dismiss()
    }

    private fun onCancelClicked() {
        viewModel.onCancelClicked()
        WebViewActivity.show(
            context,
            resources.getString(LR.string.profile_cancel_subscription),
            Settings.INFO_CANCEL_URL
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onViewDismissed()
    }

    @Composable
    private fun CancelConfirmationPage(
        rows: List<Row>,
        onStayClicked: () -> Unit,
        onCancelClicked: () -> Unit,
        onCloseClicked: () -> Unit,
    ) {
        Column(modifier = Modifier.nestedScroll(rememberViewInteropNestedScrollConnection())) {
            BottomSheetAppBar(
                title = "",
                navigationButton = NavigationButton.Close,
                onNavigationClick = onCloseClicked,
            )
            CancelConfirmationView(
                rows = rows,
                onStayClicked = onStayClicked,
                onCancelClicked = onCancelClicked,
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun CancelConfirmationView(
        rows: List<Row>,
        onStayClicked: () -> Unit,
        onCancelClicked: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        LazyColumn(modifier.padding(horizontal = 16.dp)) {
            item { Header() }
            items(rows) {
                RowItem(it)
            }
        }
        Card(elevation = 8.dp) {
            Buttons(
                onStayClicked = onStayClicked,
                onCancelClicked = onCancelClicked
            )
        }
    }

    @Composable
    private fun Header(
        modifier: Modifier = Modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(bottom = 16.dp),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_subscription_cancel),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
                modifier = modifier
                    .size(100.dp)
                    .clearAndSetSemantics { }
            )
            TextH20(
                text = stringResource(LR.string.profile_cancel_subscription),
                textAlign = TextAlign.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            TextH40(
                text = stringResource(id = LR.string.profile_cancel_confirm_subtitle),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun RowItem(
        row: Row,
        modifier: Modifier = Modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Image(
                painter = painterResource(id = row.iconResId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
                modifier = modifier
                    .padding(end = 16.dp)
            )
            TextH40(text = row.text)
        }
    }

    @Composable
    private fun Buttons(
        onStayClicked: () -> Unit,
        onCancelClicked: () -> Unit,
    ) {
        Column {
            RowButton(
                text = stringResource(LR.string.profile_cancel_confirm_stay_button_title),
                onClick = onStayClicked,
            )
            RowOutlinedButton(
                text = stringResource(LR.string.profile_cancel_confirm_cancel_button_title),
                border = BorderStroke(0.dp, Color.Transparent),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.theme.colors.support05,
                ),
                onClick = onCancelClicked,
                includePadding = false,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )
        }
    }

    @Composable
    private fun getRows() = listOf(
        Row(
            iconResId = IR.drawable.ic_subscription,
            text = stringResource(
                LR.string.profile_cancel_confirm_sub_expiry,
                viewModel.expirationDate ?: stringResource(
                    id = LR.string.profile_cancel_confirm_sub_expiry_date_fallback
                )
            )
        ),
        Row(
            iconResId = R.drawable.ic_locked_large,
            text = stringResource(LR.string.profile_cancel_confirm_item_plus)
        ),
        Row(
            iconResId = IR.drawable.folder_lock_dark,
            text = stringResource(LR.string.profile_cancel_confirm_item_folders)
        ),
        Row(
            iconResId = R.drawable.ic_upload___remove_from_cloud___menu,
            text = stringResource(LR.string.profile_cancel_confirm_item_uploads)
        ),
        Row(
            iconResId = IR.drawable.ic_website,
            text = stringResource(LR.string.profile_cancel_confirm_item_web_player)
        ),
    )

    data class Row(
        @DrawableRes val iconResId: Int,
        val text: String,
    )

    @Preview(showBackground = true)
    @Composable
    fun CancelConfirmationView_Preview(
        @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
    ) {
        AppThemeWithBackground(themeType) {
            CancelConfirmationPage(
                rows = listOf(
                    Row(
                        iconResId = R.drawable.ic_locked_large,
                        text = stringResource(LR.string.profile_cancel_confirm_item_plus)
                    ),
                    Row(
                        iconResId = IR.drawable.folder_lock_dark,
                        text = stringResource(LR.string.profile_cancel_confirm_item_folders)
                    ),
                ),
                onStayClicked = {},
                onCancelClicked = {},
                onCloseClicked = {},
            )
        }
    }
}
