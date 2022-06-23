package au.com.shiftyjelly.pocketcasts.repositories.refresh

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService

@SuppressLint("SpecifyJobSchedulerIdRange")
class RefreshPodcastsJob : JobService() {
    // This job is no longer used but we will keep the class for devices that still had it scheduled

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        jobFinished(jobParameters, false)
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return true
    }
}
