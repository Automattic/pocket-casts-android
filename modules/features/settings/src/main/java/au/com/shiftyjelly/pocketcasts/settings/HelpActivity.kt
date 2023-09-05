package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class HelpActivity : AppCompatActivity() {
    @Inject
    lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)

        setContentView(VR.layout.activity_blank_fragment)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(VR.id.container, HelpFragment())
                .commitNow()
        }
    }

    fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(VR.id.container, fragment)
            .commitNow()
    }

    fun closeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .remove(fragment)
            .commitNow()
    }
}
