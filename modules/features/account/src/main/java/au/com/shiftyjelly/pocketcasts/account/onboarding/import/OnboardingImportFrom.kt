package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.androidbrowserhelper.trusted.Utils.setStatusBarColor
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun OnboardingImportFrom(
    theme: Theme.ThemeType,
    @DrawableRes drawableRes: Int,
    title: String,
    text: String? = null,
    steps: List<String>,
    buttonText: String? = null,
    buttonClick: (() -> Unit)? = null,
    onBackPressed: () -> Unit,
) {
    rememberSystemUiController().apply {
        // Use the secondaryUI01 so the status bar matches the ThemedTopAppBar
        setStatusBarColor(MaterialTheme.theme.colors.secondaryUi01, darkIcons = !theme.defaultLightIcons)
        setNavigationBarColor(Color.Transparent, darkIcons = !theme.darkTheme)
    }

    Column(
        Modifier
            .fillMaxHeight()
    ) {

        ThemedTopAppBar(
            onNavigationClick = onBackPressed,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            TextH10(title)

            if (text != null) {
                Spacer(Modifier.height(16.dp))
                TextP40(text)
            }

            Spacer(Modifier.height(16.dp))
            NumberedList(*steps.toTypedArray())
        }
        Spacer(Modifier.weight(1f))

        if (buttonText != null && buttonClick != null) {
            RowButton(
                text = buttonText,
                onClick = buttonClick,
            )
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun NumberedList(vararg texts: String) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val numberRefs = texts.map { createRef() }
        val textRefs = texts.map { createRef() }

        val barrier = createEndBarrier(*numberRefs.toTypedArray())

        texts.forEachIndexed { index, text ->

            val numberRef = numberRefs[index]
            val textRef = textRefs[index]

            // Number
            TextP40(
                text = "${index + 1}.",
                modifier = Modifier
                    .clearAndSetSemantics {} // ignore for accessibility
                    .constrainAs(numberRef) {
                        top.linkTo(
                            anchor = if (index == 0) parent.top else textRefs[index - 1].bottom,
                            margin = if (index == 0) 0.dp else 12.dp
                        )
                        start.linkTo(parent.start)
                    }
            )

            // Indented text
            TextP40(
                text = text,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .constrainAs(textRef) {
                        top.linkTo(numberRef.top)
                        start.linkTo(barrier, 8.dp)
                        end.linkTo(parent.end)
                        height = Dimension.wrapContent
                        width = Dimension.fillToConstraints
                    }
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingImportFromPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType = themeType) {
        OnboardingImportFrom(
            theme = themeType,
            drawableRes = IR.drawable.castbox,
            title = "Import from something",
            text = "Some text to go with the title.",
            steps = listOf(
                "This is the first step.",
                "This is the second step.",
                "And this is the final step.",
            ),
            buttonText = "A button",
            buttonClick = {},
            onBackPressed = {},
        )
    }
}
