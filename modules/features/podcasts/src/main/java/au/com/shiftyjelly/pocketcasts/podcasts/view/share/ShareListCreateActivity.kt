package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class ShareListCreateActivity : AppCompatActivity() {

    @Inject lateinit var settings: Settings

    @Inject lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)
        enableEdgeToEdge(navigationBarStyle = theme.getNavigationBarStyle(this))
        setContentView(FrameLayout(this).apply { id = VR.id.container })


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(VR.id.container, ShareListCreateFragment())
                .commitNow()
        }
    }
}
