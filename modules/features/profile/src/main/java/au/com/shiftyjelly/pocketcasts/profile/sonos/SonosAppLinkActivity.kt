package au.com.shiftyjelly.pocketcasts.profile.sonos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.databinding.ActivitySonosAppLinkBinding
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class SonosAppLinkActivity : AppCompatActivity() {

    companion object {
        const val SONOS_APP_ACTIVITY_RESULT = 1007
        const val SONOS_STATE_EXTRA = "state"

        fun buildIntent(intent: Intent, context: Context): Intent {
            return Intent(context, SonosAppLinkActivity::class.java).apply {
                putExtra(SonosAppLinkActivity.SONOS_STATE_EXTRA, intent.data?.query)
            }
        }
    }

    @Inject lateinit var settings: Settings
    @Inject lateinit var theme: Theme
    @Inject lateinit var serverManager: ServerManager

    private lateinit var sonosState: String
    private lateinit var binding: ActivitySonosAppLinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(theme.activeTheme.resourceId)

        binding = ActivitySonosAppLinkBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val toolbar = binding.toolbar
        toolbar.setup(
            title = getString(LR.string.profile_sonos_connect_to),
            navigationIcon = NavigationIcon.Close,
            onNavigationClick = { finish() },
            activity = this,
            theme = theme
        )

        intent.getStringExtra(SONOS_STATE_EXTRA)?.let {
            sonosState = it
        } ?: run { finish() }

        binding.connectBtn.setOnClickListener {
            val email = settings.getSyncEmail()
            val password = settings.getSyncPassword()
            if (email != null && password != null) {
                connectWithSonos(email, password)
            } else {
                setupSyncing()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.sonosImage.setImageResource(if (theme.isDarkTheme) IR.drawable.sonos_dark else IR.drawable.sonos_light)

        if (settings.isLoggedIn()) {
            binding.explanationText.setText(LR.string.profile_sonos_connect_account)
            binding.connectBtn.setText(LR.string.profile_sonos_connect)
        } else {
            binding.explanationText.setText(LR.string.profile_sonos_need_account)
            binding.connectBtn.setText(LR.string.profile_sonos_setup_account)
        }
    }

    private fun setupSyncing() {
        startActivity(Intent(this, AccountActivity::class.java))
    }

    private fun connectWithSonos(email: String, password: String) {
        binding.connectBtn.setText(LR.string.profile_sonos_connecting)
        serverManager.obtainThirdPartyToken(
            email, password, "sonos",
            object : ServerCallback<String> {
                override fun dataReturned(result: String?) {
                    result ?: return
                    tokenObtained(result)
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    tokenObtainFailed()
                }
            }
        )
    }

    private fun tokenObtained(token: String) {
        try {
            val result = Intent()
            result.putExtra("code", URLEncoder.encode(token, "UTF-8"))
            val state = sonosState.replace("state=".toRegex(), "")
            result.putExtra("state", state)
            setResult(SONOS_APP_ACTIVITY_RESULT, result)
            finish()
        } catch (e: UnsupportedEncodingException) {
            Timber.e(e)
        }
    }

    private fun tokenObtainFailed() {
        binding.connectBtn.setText(LR.string.profile_sonos_retry)
        UiUtil.displayAlert(this, getString(LR.string.profile_sonos_linking_failed), getString(LR.string.profile_sonos_linking_failed_summary), null)
    }
}
