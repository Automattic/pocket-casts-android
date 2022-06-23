package au.com.shiftyjelly.pocketcasts.utils

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler

/**
 * SchedulerProvider
 * Convenience class for switching out the scheduler for a test scheduler
 * when running under the testing environment
 */
object SchedulerProvider {
    val testScheduler = TestScheduler()

    /**
     * io
     * Returns the default io scheduler normally but returns testScheduler if running
     * under UI tests
     */
    val io: Scheduler
        get() = if (IS_RUNNING_UNDER_TEST) testScheduler else Schedulers.io()

    /**
     * io
     * Returns the default android main thread scheduler normally but returns testScheduler if running
     * under UI tests
     */
    val mainThread: Scheduler
        get() = if (IS_RUNNING_UNDER_TEST) testScheduler else AndroidSchedulers.mainThread()
}
