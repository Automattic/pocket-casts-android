package au.com.shiftyjelly.pocketcasts

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TvApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree())
        }
    }
}
