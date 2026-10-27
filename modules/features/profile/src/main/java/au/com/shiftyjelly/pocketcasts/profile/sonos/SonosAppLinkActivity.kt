package au.com.shiftyjelly.pocketcasts.profile.sonos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import com.google.android.material.R as MR

@AndroidEntryPoint
class SonosAppLinkActivity :
    AppCompatActivity(),
    CoroutineScope {

    companion object {
        const val SONOS_APP_ACTIVITY_RESULT = 1007
        const val SONOS_STATE_EXTRA = "state"

        fun buildIntent(state: String, context: Context): Intent {
            return Intent(context, SonosAppLinkActivity::class.java).apply {
                putExtra(SONOS_STATE_EXTRA, state)
            }
        }
    }

    @Inject lateinit var settings: Settings

    @Inject lateinit var theme: Theme

    @Inject lateinit var syncManager: SyncManager

    private lateinit var sonosState: String

    private var imageRes by mutableIntStateOf(IR.drawable.sonos_light)

    private var explanationRes by mutableIntStateOf(LR.string.profile_sonos_description)

    private var buttonTextRes by mutableIntStateOf(LR.string.profile_sonos_connect)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(theme.activeTheme.resourceId)

        val textColors = SonosTextColors(
            title = Color(textAppearanceColor(UR.attr.textH3)),
            explanation = Color(textAppearanceColor(UR.attr.textBody1)),
            button = Color(themeColor(MR.attr.colorOnPrimary)),
        )
        setContent {
            AppTheme(theme.activeTheme) {
                SonosAppLinkPage(
                    imageRes = imageRes,
                    explanation = stringResource(explanationRes),
                    buttonText = stringResource(buttonTextRes),
                    textColors = textColors,
                    onCloseClick = ::finish,
                    onConnectClick = ::onConnectClick,
                    toolbarModifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(this@SonosAppLinkActivity) })
                    },
                )
            }
        }

        intent.getStringExtra(SONOS_STATE_EXTRA)?.let {
            sonosState = it
        } ?: run { finish() }
    }

    override fun onResume() {
        super.onResume()

        imageRes = if (theme.isDarkTheme) IR.drawable.sonos_dark else IR.drawable.sonos_light

        if (syncManager.isLoggedIn()) {
            explanationRes = LR.string.profile_sonos_connect_account
            buttonTextRes = LR.string.profile_sonos_connect
        } else {
            explanationRes = LR.string.profile_sonos_need_account
            buttonTextRes = LR.string.profile_sonos_setup_account
        }
    }

    private fun onConnectClick() {
        if (syncManager.isLoggedIn()) {
            launch {
                connectWithSonos()
            }
        } else {
            setupSyncing()
        }
    }

    private fun setupSyncing() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    private suspend fun connectWithSonos() {
        try {
            buttonTextRes = LR.string.profile_sonos_connecting

            val response = syncManager.exchangeSonos()
            val sonosToken = response.accessToken

            val code = URLEncoder.encode(sonosToken.value, "UTF-8")
            val state = sonosState

            val result = Intent().apply {
                putExtra("code", code)
                putExtra("state", state)
            }

            setResult(SONOS_APP_ACTIVITY_RESULT, result)
            finish()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_CRASH, e, "Failed to link Sonos")

            buttonTextRes = LR.string.profile_sonos_retry
            UiUtil.displayAlert(this, getString(LR.string.profile_sonos_linking_failed), getString(LR.string.profile_sonos_linking_failed_summary), null)
        }
    }

    private fun textAppearanceColor(@AttrRes textAppearanceAttr: Int): Int {
        val textAppearance = obtainStyledAttributes(intArrayOf(textAppearanceAttr)).use { it.getResourceId(0, 0) }
        return obtainStyledAttributes(textAppearance, intArrayOf(android.R.attr.textColor)).use { it.getColor(0, 0) }
    }

    private fun themeColor(@AttrRes colorAttr: Int): Int {
        return obtainStyledAttributes(intArrayOf(colorAttr)).use { it.getColor(0, 0) }
    }
}
