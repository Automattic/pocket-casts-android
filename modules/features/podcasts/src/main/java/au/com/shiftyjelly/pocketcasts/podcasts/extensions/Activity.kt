package au.com.shiftyjelly.pocketcasts.podcasts.extensions

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.getSupportActionBar(): ActionBar? {
    return if (this is AppCompatActivity) this.supportActionBar else null
}

fun FragmentActivity.setSupportActionBar(toolbar: Toolbar): ActionBar? {
    if (this is AppCompatActivity) {
        this.setSupportActionBar(toolbar)
    }
    return getSupportActionBar()
}
