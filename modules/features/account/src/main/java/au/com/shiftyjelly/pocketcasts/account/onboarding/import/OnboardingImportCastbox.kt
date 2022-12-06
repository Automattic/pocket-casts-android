package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingImportCastbox(
    onBackPressed: () -> Unit
) {
    Column {

        ThemedTopAppBar(
            onNavigationClick = onBackPressed,
        )

        Column(Modifier.padding(horizontal = 24.dp)) {

            Image(
                painter = painterResource(IR.drawable.castbox),
                contentDescription = null,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            TextH10(stringResource(LR.string.onboarding_import_from_castbox))

            Spacer(Modifier.height(16.dp))

            NumberedList(
                stringResource(LR.string.onboarding_import_from_castbox_step_1),
                stringResource(LR.string.onboarding_import_from_castbox_step_2),
                stringResource(LR.string.onboarding_import_from_castbox_step_3),
                stringResource(LR.string.onboarding_import_from_castbox_step_4),
                stringResource(LR.string.onboarding_import_from_castbox_step_5),
                stringResource(LR.string.onboarding_import_from_castbox_step_6),
                stringResource(LR.string.onboarding_import_from_castbox_step_7),
            )
        }

        Spacer(Modifier.weight(1f))

        RowButton(
            text = stringResource(LR.string.onboarding_import_from_castbox_open),
            onClick = {},
        )
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
                modifier = Modifier.constrainAs(numberRef) {
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
fun OnboardingImportCastboxPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingImportCastbox(
            onBackPressed = {}
        )
    }
}
