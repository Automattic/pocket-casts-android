package au.com.shiftyjelly.pocketcasts.profile.sharing

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class ShareProfileActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)
        enableEdgeToEdge(navigationBarStyle = theme.getNavigationBarStyle(this))

        setContentView(VR.layout.activity_blank_fragment)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(VR.id.container, ShareProfileFragment())
                .commitNow()
        }
    }
}
