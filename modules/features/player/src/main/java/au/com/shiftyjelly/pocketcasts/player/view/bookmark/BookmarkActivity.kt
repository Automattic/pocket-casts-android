package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.commitNow
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class BookmarkActivity : AppCompatActivity() {
    companion object {
        private const val NEW_INSTANCE_KEY = "new_instance_key"

        fun launchIntent(context: Context, args: BookmarkArguments): Intent {
            return Intent(context, BookmarkActivity::class.java).putExtra(NEW_INSTANCE_KEY, args)
        }
    }

    private val args
        get() = requireNotNull(IntentCompat.getParcelableExtra(intent, NEW_INSTANCE_KEY, BookmarkArguments::class.java)) {
            "Missing input parameters"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = VR.id.container })


        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(VR.id.container, BookmarkFragment.newInstance(args))
            }
        }
    }
}
