package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.qr.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles

@Composable
fun TvSignInQrContent(
    userCode: List<String>,
    verificationUriComplete: String,
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QrCode(content = verificationUriComplete)
            StepList(steps = steps)
        }
        CodeRow(
            userCode = userCode,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val qrPainter = rememberQrPainter(content = content, size = QrSize)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Image(
            painter = qrPainter,
            contentDescription = null,
            modifier = Modifier.size(QrSize),
        )
    }
}

@Composable
private fun StepList(steps: List<String>, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        steps.forEachIndexed { index, step ->
            StepRow(number = index + 1, text = step)
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(TvColors.BackgroundActive20, CircleShape),
        ) {
            Text(
                text = number.toString(),
                color = TvColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = text,
            color = TvColors.TextPrimary,
            style = TvTextStyles.Title3.copy(textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun CodeRow(userCode: List<String>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        userCode.forEach { character ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(TvColors.BackgroundActive20, CircleShape),
            ) {
                Text(
                    text = character,
                    color = TvColors.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

fun verificationDisplayUrl(verificationUri: String): String {
    return verificationUri
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
}

private val QrSize = 132.dp
