package au.com.shiftyjelly.pocketcasts.compose.dialogs

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonGroup
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun OptionsDialogComponent(title: String?, @ColorInt iconColor: Int?, options: List<OptionsDialogOption>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        OptionsDialogHeader(title)
        options.forEachIndexed { index, option ->
            Column {
                OptionsDialogRow(option = option, iconColor = option.imageColor ?: iconColor, index = index)
                if (index != options.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun OptionsDialogHeader(title: String?, modifier: Modifier = Modifier) {
    if (title == null) {
        return
    }
    Text(
        text = title.uppercase(),
        fontSize = 15.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.support01,
        modifier = modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun OptionsDialogRow(option: OptionsDialogOption, @ColorInt iconColor: Int?, index: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(74.dp)
            .fillMaxWidth()
            .clickable(enabled = option.click != null) { option.click?.invoke() }
            .testTag("option_$index")
            .background(MaterialTheme.theme.colors.primaryUi01)
    ) {
        if (option.imageId != null) {
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                painter = painterResource(id = option.imageId),
                contentDescription = null,
                tint = if (iconColor == null) MaterialTheme.theme.colors.primaryIcon01 else Color(iconColor),
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
            )
        }
        Text(
            text = if (option.titleId != null) stringResource(option.titleId) else (option.titleString ?: ""),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).sp,
            color = if (option.titleColor == null) MaterialTheme.theme.colors.primaryText01 else Color(option.titleColor),
            modifier = Modifier.padding(start = 20.dp, end = 10.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        ToggleButtonGroup(option.toggleOptions)
        if (option.valueId != null) {
            Text(
                text = stringResource(option.valueId),
                fontSize = 16.sp,
                color = MaterialTheme.theme.colors.primaryText02,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).sp,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        if (option.checked && option.onSwitch == null) {
            Icon(
                painter = painterResource(id = IR.drawable.ic_tick),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon01,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
    }
}
