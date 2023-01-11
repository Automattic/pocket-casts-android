package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.sync.PromoCodeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class PromoCodeViewModel @Inject constructor(
    private val syncServerManager: SyncServerManager,
    private val settings: Settings,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {
    sealed class ViewState {
        object Loading : ViewState()
        data class Success(val response: PromoCodeResponse) : ViewState()
        data class NotSignedIn(val response: PromoCodeResponse) : ViewState()
        data class Failed(val isSignedIn: Boolean, val title: String, val errorMessage: String, @DrawableRes val errorImageRes: Int, @DrawableRes val errorOverlayRes: Int? = null, val shouldShowSignup: Boolean) : ViewState()
    }

    private var disposable: Disposable? = null
    val state: MutableLiveData<ViewState> = MutableLiveData(ViewState.Loading)

    fun setup(code: String, context: Context) {
        val signedInFlow = Single.defer<ViewState> { syncServerManager.redeemPromoCode(code).map { ViewState.Success(it) } }
            .flatMap { viewState ->
                subscriptionManager.getSubscriptionStatus(allowCache = false).map { viewState } // Force reloading of the new subscription status
            }
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn(errorHandler(isSignedIn = true, resources = context.resources))
            .toFlowable()

        val signedOutFlow = syncServerManager.validatePromoCode(code)
            .observeOn(AndroidSchedulers.mainThread())
            .map<ViewState> { ViewState.NotSignedIn(it) }
            .onErrorReturn(errorHandler(isSignedIn = false, resources = context.resources))
            .toFlowable()

        disposable?.dispose()
        disposable = settings.isLoggedInObservable.toFlowable(BackpressureStrategy.LATEST)
            .observeOn(Schedulers.io())
            .takeUntil { it } // Once we are signed in we don't want to be notified for other changes to the account like being upgraded to plus
            .switchMap { signedIn ->
                if (signedIn) {
                    signedInFlow.startWith(ViewState.Loading as ViewState)
                } else {
                    signedOutFlow
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .startWith(ViewState.Loading)
            .doOnNext { state.postValue(it) }
            .subscribe()
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }

    private fun errorHandler(isSignedIn: Boolean, resources: Resources): Function<Throwable, ViewState> {
        return Function {
            when (it) {
                is HttpException -> {
                    val errorResponse = it.parseErrorResponse()
                    var message = errorResponse?.messageLocalized(resources) ?: "Unknown error"
                    if (it.code() == 404) {
                        message = if (isSignedIn) {
                            "$message\nYou’re welcome to sign up for Pocket Casts Plus anyway, or continue using all the great features of your free account."
                        } else {
                            "$message\nYou’re welcome to sign up for Pocket Casts Plus anyway, create a free account, or just dive right in."
                        }
                    }

                    val title = when (it.code()) {
                        400 -> "Code already used"
                        404 -> "Promotion Expired or Invalid"
                        409 -> "You already have a Pocket Casts Plus account"
                        else -> "Error"
                    }
                    val images = when (it.code()) {
                        400 -> R.drawable.ic_promocode_expired to R.drawable.ic_promo_expired_overlay
                        404 -> R.drawable.ic_promocode_expired to R.drawable.ic_promo_expired_overlay
                        409 -> R.drawable.ic_plus_account to null
                        else -> R.drawable.ic_promocode_expired to R.drawable.ic_promo_expired_overlay
                    }
                    ViewState.Failed(isSignedIn, title, message, images.first, images.second, shouldShowSignup = it.code() == 404)
                }
                else -> {
                    ViewState.Failed(isSignedIn, "Error", "An unknown error occurred.\n${it.message ?: it.toString()}", R.drawable.ic_promocode_expired, R.drawable.ic_promo_expired_overlay, shouldShowSignup = false)
                }
            }
        }
    }
}
