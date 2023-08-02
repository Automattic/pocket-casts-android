package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class LoginWithGoogleScreenViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var googleSignInClient: GoogleSignInClient
    @Mock
    private lateinit var podcastManager: PodcastManager
    @Mock
    private lateinit var syncManager: SyncManager
    private lateinit var testSubject: LoginWithGoogleScreenViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testSubject = LoginWithGoogleScreenViewModel(
            googleSignInClient = googleSignInClient,
            podcastManager = podcastManager,
            syncManager = syncManager
        )
    }

    @Test
    fun `test signing in with Google Successfully`() = runTest {
        val googleSignInAccount = mock(GoogleSignInAccount::class.java)
        testSubject.onSignedIn(googleSignInAccount)
        testSubject.state.test {
            assertEquals(awaitItem().googleSignInAccount, googleSignInAccount)
        }
    }

    @Test
    fun `test clearing previous sign in`() = runBlocking {
        testSubject.clearPreviousSignIn()
        testSubject.state.test {
            assertNull(awaitItem().googleSignInAccount)
        }
    }
}
