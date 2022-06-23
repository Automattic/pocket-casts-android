package au.com.shiftyjelly.pocketcasts.repositories.download

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService

@SuppressLint("SpecifyJobSchedulerIdRange")
class UpdateEpisodeDetailsJob : JobService() {
    // This job is no longer used but we will keep the class for devices that still had it scheduled

    override fun onStartJob(params: JobParameters?): Boolean {
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}
