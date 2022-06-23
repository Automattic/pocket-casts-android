package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DoneViewModel @Inject constructor() : ViewModel() {

    val title = MutableLiveData<String>()
    val detail = MutableLiveData<String>()
    val imageRef = MutableLiveData<Int>()

    fun updateTitle(value: String) {
        title.value = value
    }

    fun updateDetail(value: String) {
        detail.value = value
    }

    fun updateImage(resid: Int) {
        imageRef.value = resid
    }
}
