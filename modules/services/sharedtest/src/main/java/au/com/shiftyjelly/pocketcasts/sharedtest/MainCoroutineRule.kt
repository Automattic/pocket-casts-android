package au.com.shiftyjelly.pocketcasts.sharedtest

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.functions.Function
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    // This is needed due to RxJava and Coroutines interop
    private val immediateScheduler = Schedulers.trampoline()
    private val androidSchedulerHandler = Function<Callable<Scheduler>, Scheduler> { immediateScheduler }
    private val schedulerHandler = Function<Scheduler, Scheduler> { immediateScheduler }

    override fun starting(description: Description) {
        RxJavaPlugins.setIoSchedulerHandler(schedulerHandler)
        RxJavaPlugins.setComputationSchedulerHandler(schedulerHandler)
        RxJavaPlugins.setNewThreadSchedulerHandler(schedulerHandler)
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(androidSchedulerHandler)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }
}
