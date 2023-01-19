package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShareListCreateActivity : AppCompatActivity() {

    @Inject lateinit var settings: Settings
    @Inject lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)

        setContentView(R.layout.activity_blank_fragment)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ShareListCreateFragment())
                .commitNow()
        }
    }
}
