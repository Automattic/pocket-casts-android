package au.com.shiftyjelly.pocketcasts.navigation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable

/**
 * Handles subscriptions that would traditionally be handled by the Activity.
 */
internal class ActivityDelegate(
    private val fragmentContainer: Int,
    private val modalContainer: Int,
    fragmentManagerFactory: () -> FragmentManager,
    private val lifecycle: Lifecycle,
    val bottomNavigationView: BottomNavigationView,
    private val bottomNavigator: BottomNavigator
) : LifecycleObserver {
    fun clear() {
        bin.clear()
        lifecycle.removeObserver(this)
    }

    private val bin = CompositeDisposable()
    val fragmentManager = fragmentManagerFactory()

    init {
        lifecycle.addObserver(this)
    }

    @Suppress("DEPRECATION")
    @androidx.lifecycle.OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onActivityStart() {
        bin.clear()
        val fragmentTransactionHandler =
            FragmentTransactionHandler(fragmentManager, fragmentContainer, modalContainer)
        bottomNavigator.fragmentTransactionPublisher
            .subscribe { command ->
                fragmentTransactionHandler.handle(command)
            }
            .into(bin)

        setupBottomNavigationView()
    }

    private fun setupBottomNavigationView() {
        // Don't trigger onNavigationItemSelected when setSelectedItem was called programatically.
        var programmaticSelect = false
        bottomNavigationView.setOnItemSelectedListener {
            if (programmaticSelect) {
                programmaticSelect = false
            } else {
                bottomNavigator.onNavigationItemSelected(it)
            }
            true
        }

        bottomNavigator.bottomnavViewSetSelectedItemObservable
            .subscribe { currentTab ->
                if (bottomNavigationView.selectedItemId != currentTab) {
                    programmaticSelect = true
                    bottomNavigationView.selectedItemId = currentTab
                }
            }
            .into(bin)
    }

    @Suppress("DEPRECATION")
    @androidx.lifecycle.OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onActivityStop() {
        bin.clear()
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
    }
}
