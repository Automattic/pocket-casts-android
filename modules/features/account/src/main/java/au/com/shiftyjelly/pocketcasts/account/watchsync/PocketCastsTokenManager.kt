package au.com.shiftyjelly.pocketcasts.account.watchsync

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.security.MessageDigest

class PocketCastsTokenManager(private val context: Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val credentialManager = CredentialManager.create(context)

    suspend fun persistAndShare(
        refreshToken: String,
    ) {
        saveToCredentialManager(refreshToken)
        shareCustomCredentials(refreshToken)
    }

    private suspend fun shareCustomCredentials(refreshToken: String) {
        val dataMap = DataMap().apply {
            putString("refreshToken", refreshToken)
            putLong("timestamp", System.currentTimeMillis())
            putString("signature", appSignature())
        }

        val putRequest = PutDataMapRequest.create("/custom_auth_v2").run {
            dataMap.putAll(dataMap)
            setUrgent()
            asPutDataRequest()
        }

        try {
            Tasks.await(dataClient.putDataItem(putRequest))
            Log.d("Auth", "Custom credentials shared successfully")
        } catch (e: Exception) {
            Log.e("Auth", "Error sharing custom credentials", e)
        }
    }

    private suspend fun saveToCredentialManager(refreshToken: String) {
        val request = CreateCredentialRequest.createFrom(
            type = PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
            credentialData = Bundle().apply {
                putString("pocketcasts.credentials.refreshToken", refreshToken)
            },
            candidateQueryData = Bundle(),
            requireSystemProvider = false,
            origin = "app",
        )

        try {
            credentialManager.createCredential(context, request)
            Log.d("Auth", "Saved credential (v1.6.0) to Credential Manager")
        } catch (e: CreateCredentialException) {
            Log.e("Auth", "Failed to save credential", e)
        }
    }

    private fun appSignature(): String {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        val certBytes = packageInfo.signatures.orEmpty()
            .first()
            .toByteArray()

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}