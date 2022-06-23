package au.com.shiftyjelly.pocketcasts.navigation

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

internal fun Disposable.into(compositeDisposable: CompositeDisposable?) {
    compositeDisposable?.add(this)
}

internal fun Disposable.into(map: MutableMap<String, Disposable>, key: String): Disposable {
    map[key] = this
    return this
}
