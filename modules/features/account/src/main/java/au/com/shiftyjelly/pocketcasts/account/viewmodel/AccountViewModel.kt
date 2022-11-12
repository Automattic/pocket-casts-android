package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class AccountViewModel : ViewModel(), CoroutineScope {

    val email = MutableLiveData<String>().apply { postValue("") }
    val password = MutableLiveData<String>().apply { postValue("") }

    val confirmationMessages = MutableLiveData<Pair<String, String>>().apply { postValue(Pair("", "")) }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    companion object {
        fun isEmailValid(value: String?): Boolean {
            return value != null && Patterns.EMAIL_ADDRESS.matcher(value).matches()
        }

        fun isPasswordValid(value: String?): Boolean {
            return value != null && value.length >= 6
        }
    }
}
