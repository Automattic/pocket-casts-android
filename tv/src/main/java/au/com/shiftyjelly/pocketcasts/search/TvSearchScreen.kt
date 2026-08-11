package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors

@Composable
fun TvSearchScreen(
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        TvSearchField(query = query)
        Spacer(modifier = Modifier.height(40.dp))
        TvSearchKeyboard(
            onCharacter = { query += it },
            onSpace = { query += ' ' },
            onDelete = { query = query.dropLast(1) },
            onSubmit = {},
            autoFocus = true,
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchScreenPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchScreen()
        }
    }
}
