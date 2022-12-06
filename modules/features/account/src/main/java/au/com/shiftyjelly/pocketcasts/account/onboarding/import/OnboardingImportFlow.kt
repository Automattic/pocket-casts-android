package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40

object OnboardingImportFlow {

    const val route = "onboardingImportFlow"

    private const val start = "start"
    private const val castbox = "castbox"
    private const val otherApps = "otherApps"

    fun NavGraphBuilder.importFlowGraph(navController: NavController) {
        navigation(
            route = this@OnboardingImportFlow.route,
            startDestination = start,
        ) {
            composable(start) {
                OnboardingImportStartPage(
                    onCastboxClicked = { navController.navigate(castbox) },
                    onOtherAppsClicked = { navController.navigate(otherApps) },
                    onBackPressed = { navController.popBackStack() },
                )
            }

            composable(castbox) {
                OnboardingImportCastbox(
                    onBackPressed = { /* TODO */ },
                )
            }

            composable(otherApps) {
                OnboardingImportOtherApps(
                    onBackPressed = { /* TODO */ },
                )
            }
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
