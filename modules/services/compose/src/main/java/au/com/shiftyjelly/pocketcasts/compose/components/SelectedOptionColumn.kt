package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun <T : Any> SelectedOptionColumn(
    options: List<T>,
    selectedOption: T?,
    optionLabel: @Composable (T) -> String,
    onClickOption: (T) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp),
    ) {
        if (title != null) {
            OptionTitle(text = title)
        }
        options.forEachIndexed { index, option ->
            OptionRow(
                text = optionLabel(option),
                isSelected = option == selectedOption,
                showDivider = index != options.lastIndex,
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = { onClickOption(option) },
                ),
            )
        }
    }
}

@Composable
private fun OptionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        fontSize = 15.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.support01,
        modifier = modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun OptionRow(
    text: String,
    isSelected: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp),
        ) {
            TextH30(
                text = text,
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(IR.drawable.ic_tick),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryIcon01,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SelectOptionColumnWithoutTitlePreview() {
    AppThemeWithBackground(themeType = ThemeType.LIGHT) {
        SelectedOptionColumn(
            options = List(4) { it },
            selectedOption = 0,
            optionLabel = { it.toString() },
            onClickOption = {},
        )
    }
}

@Preview
@Composable
private fun SelectOptionColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SelectedOptionColumn(
            title = "Title",
            options = List(4) { it },
            selectedOption = 0,
            optionLabel = { it.toString() },
            onClickOption = {},
        )
    }
}
