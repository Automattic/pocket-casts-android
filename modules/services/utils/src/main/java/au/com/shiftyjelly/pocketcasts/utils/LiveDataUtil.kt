package au.com.shiftyjelly.pocketcasts.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

// Combine 2 live data in to 1 pair
fun <A, B> LiveData<A>.combineLatest(b: LiveData<B>): LiveData<Pair<A, B>> {
    return MediatorLiveData<Pair<A, B>>().apply {
        var lastA: A? = null
        var lastB: B? = null

        addSource(this@combineLatest) {
            if (it == null && value != null) value = null
            lastA = it
            if (lastA != null && lastB != null) value = lastA!! to lastB!!
        }

        addSource(b) {
            if (it == null && value != null) value = null
            lastB = it
            if (lastA != null && lastB != null) value = lastA!! to lastB!!
        }
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(
        lifecycleOwner,
        object : Observer<T> {
            override fun onChanged(value: T) {
                removeObserver(this)
                observer.onChanged(value)
            }
        }
    )
}
