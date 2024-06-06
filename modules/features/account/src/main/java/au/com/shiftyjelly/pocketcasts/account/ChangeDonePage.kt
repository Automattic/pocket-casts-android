package au.com.shiftyjelly.pocketcasts.account

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_close
import au.com.shiftyjelly.pocketcasts.localization.R.string.close
import au.com.shiftyjelly.pocketcasts.localization.R.string.done
import au.com.shiftyjelly.pocketcasts.localization.R.string.profile_confirm

@Composable
fun ChangeDonePage(
    modifier: Modifier = Modifier,
    viewModel: DoneViewModel,
    closeForm: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Image(
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            painter = painterResource(id = ic_close),
            contentDescription = stringResource(id = close),
            modifier = modifier
                .padding(start = 24.dp, top = 24.dp)
                .width(24.dp)
                .height(24.dp)
                .clickable { closeForm() },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(if (isPortrait) PaddingValues(vertical = 16.dp) else PaddingValues(horizontal = 32.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val state by viewModel.state.collectAsState()

            val imageResource = when (state) {
                is DoneViewModel.State.Empty -> null
                is DoneViewModel.State.SuccessFullChangedEmail -> painterResource(id = (state as DoneViewModel.State.SuccessFullChangedEmail).imageResourceId)
                is DoneViewModel.State.SuccessFullChangedPassword -> painterResource(id = (state as DoneViewModel.State.SuccessFullChangedPassword).imageResourceId)
            }

            imageResource?.let {
                Image(
                    painter = it,
                    contentDescription = stringResource(id = done),
                    modifier = if (isPortrait) {
                        Modifier.padding(bottom = 16.dp).size(140.dp)
                    } else {
                        Modifier.size(120.dp)
                    },
                )
            }

            val primaryText = when (state) {
                is DoneViewModel.State.Empty -> null
                is DoneViewModel.State.SuccessFullChangedEmail -> stringResource(id = (state as DoneViewModel.State.SuccessFullChangedEmail).tittleResourceId)
                is DoneViewModel.State.SuccessFullChangedPassword -> stringResource(id = (state as DoneViewModel.State.SuccessFullChangedPassword).tittleResourceId)
            }

            primaryText?.let {
                Text(
                    text = it,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = if (isPortrait) {
                        Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)
                    } else {
                        Modifier.padding(start = 16.dp, end = 16.dp)
                    },
                )
            }

            val secondaryText = when (state) {
                is DoneViewModel.State.Empty -> null
                is DoneViewModel.State.SuccessFullChangedEmail -> (state as DoneViewModel.State.SuccessFullChangedEmail).detail
                is DoneViewModel.State.SuccessFullChangedPassword -> (state as DoneViewModel.State.SuccessFullChangedPassword).detail
            }

            secondaryText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.theme.colors.primaryText02,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    modifier = if (isPortrait) {
                        Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    } else {
                        Modifier.padding(start = 8.dp, end = 8.dp)
                    },
                )
            }

            RowOutlinedButton(
                text = stringResource(id = profile_confirm),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                ),
                onClick = { closeForm() },
                includePadding = false,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            )
        }
    }
}
