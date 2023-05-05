package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CornerRadiusInDp = 24.dp
@Composable
fun StyledToggle(
    items: List<String>,
    modifier: Modifier = Modifier,
    defaultSelectedItemIndex: Int = 0,
    cornerRadius: Dp = CornerRadiusInDp,
    onItemSelected: (selectedItemIndex: Int) -> Unit,
) {
    var selectedIndex by remember { mutableStateOf(defaultSelectedItemIndex) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = .16f))
            .padding(horizontal = 8.dp),
    ) {
        Row {
            items.forEachIndexed { index, item ->
                OutlinedButton(
                    shape = RoundedCornerShape(cornerRadius),
                    border = null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selectedIndex == index) Color.White else Color.Transparent
                    ),
                    onClick = {
                        selectedIndex = index
                        onItemSelected(selectedIndex)
                    },
                ) {
                    TextH40(
                        text = item,
                        color = if (selectedIndex == index) Color.Black else Color.White,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingPatonFeatureCardPreview() {
    StyledToggle(
        items = listOf("Yearly", "Monthly"),
        onItemSelected = {}
    )
}
