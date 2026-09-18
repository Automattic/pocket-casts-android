package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.commitNow
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class BookmarkTranscriptEditActivity : AppCompatActivity() {
    companion object {
        private const val NEW_INSTANCE_KEY = "new_instance_key"
        const val RESULT_PASSAGE = "result_passage"
        const val RESULT_PASSAGE_LOCATION = "result_passage_location"

        fun launchIntent(context: Context, args: BookmarkTranscriptEditArguments): Intent {
            return Intent(context, BookmarkTranscriptEditActivity::class.java).putExtra(NEW_INSTANCE_KEY, args)
        }

        fun resultIntent(passage: String, passageLocation: Int): Intent {
            return Intent()
                .putExtra(RESULT_PASSAGE, passage)
                .putExtra(RESULT_PASSAGE_LOCATION, passageLocation)
        }
    }

    private val args
        get() = requireNotNull(IntentCompat.getParcelableExtra(intent, NEW_INSTANCE_KEY, BookmarkTranscriptEditArguments::class.java)) {
            "Missing input parameters"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(VR.layout.activity_blank_fragment)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(VR.id.container, BookmarkTranscriptEditFragment.newInstance(args))
            }
        }
    }
}
