package au.com.shiftyjelly.pocketcasts.models.type

import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

data class Username(
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
) {
    companion object {
        fun from(account: GoogleSignInAccount) = Username(
            firstName = account.givenName,
            lastName = account.familyName,
            displayName = account.displayName,
        )

        fun from(credential: SignInCredential) = Username(
            firstName = credential.givenName,
            lastName = credential.familyName,
            displayName = credential.displayName,

        )
    }
}
