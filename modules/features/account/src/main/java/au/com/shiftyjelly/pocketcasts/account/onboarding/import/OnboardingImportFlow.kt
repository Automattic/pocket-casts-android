package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40

@Composable
fun OnboardingImportFlow(
    onBackPressed: () -> Unit,
) {

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = OnboardingImportNavRoute.start
    ) {
        composable(OnboardingImportNavRoute.start) {
            OnboardingImportStartPage(
                onCastboxClicked = { navController.navigate(OnboardingImportNavRoute.castbox) },
                onOtherAppsClicked = { navController.navigate(OnboardingImportNavRoute.otherApps) },
                onBackPressed = { navController.popBackStack() },
            )
        }

        composable(OnboardingImportNavRoute.castbox) {
            OnboardingImportCastbox(
                onBackPressed = { /* TODO */ },
            )
        }

        composable(OnboardingImportNavRoute.otherApps) {
            OnboardingImportOtherApps(
                onBackPressed = { /* TODO */ },
            )
        }
    }
}

@Composable
fun NumberedList(vararg texts: String) {
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

private object OnboardingImportNavRoute {
    const val start = "start"
    const val castbox = "castbox"
    const val otherApps = "otherApps"
}
