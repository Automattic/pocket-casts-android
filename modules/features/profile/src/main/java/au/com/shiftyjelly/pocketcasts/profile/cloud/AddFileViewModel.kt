package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddFileViewModel @Inject constructor(
    userManager: UserManager,
    val userEpisodeManager: UserEpisodeManager
) : ViewModel() {

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()

    suspend fun updateImageOnServer(userEpisode: UserEpisode, imageFile: File) = withContext(Dispatchers.IO) {
        userEpisodeManager.uploadImageToServer(userEpisode, imageFile).await()
    }

    suspend fun updateFileMetadataOnServer(userEpisode: UserEpisode) {
        userEpisodeManager.updateFiles(listOf(userEpisode))
    }

    suspend fun deleteImageFromServer(userEpisode: UserEpisode) {
        userEpisodeManager.deleteImageFromServer(userEpisode)
    }
}
