package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import com.pocketcasts.service.api.UuidListResponse
import com.pocketcasts.service.api.UuidsRequest
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory
import retrofit2.create

class SyncServiceManagerWhatsNewTest {
    private val server = MockWebServer()
    private val token = AccessToken("token")
    private lateinit var manager: SyncServiceManager

    @Before
    fun setUp() {
        server.start()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ProtoConverterFactory.create())
            .build()
            .create<SyncService>()
        manager = SyncServiceManager(service, mock(), Lazy { mock() }, AppPlatform.Phone)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `asks which of the messages the account has read`() = runTest {
        val body = UuidListResponse.newBuilder().addUuids("m1").build()
        server.enqueue(MockResponse().setBody(Buffer().write(body.toByteArray())))

        val read = manager.getWhatsNewReadMessageIds(listOf("m1", "m2"), token)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/user/whats_new/read_state/list", request.path)
        assertEquals("Bearer token", request.getHeader("Authorization"))
        assertEquals(listOf("m1", "m2"), UuidsRequest.parseFrom(request.body.readByteArray()).uuidsList)
        assertEquals(setOf("m1"), read)
    }

    @Test
    fun `marks messages read for the account`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        manager.markWhatsNewAsRead(listOf("m1"), token)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/user/whats_new/read", request.path)
        assertEquals(listOf("m1"), UuidsRequest.parseFrom(request.body.readByteArray()).uuidsList)
    }

    @Test
    fun `marks messages unread for the account`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        manager.markWhatsNewAsUnread(listOf("m1"), token)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/user/whats_new/unread", request.path)
    }

    @Test
    fun `a rejected update is reported as a failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))

        val error = runCatching { manager.markWhatsNewAsRead(listOf("not-a-uuid"), token) }.exceptionOrNull()

        assertEquals(400, (error as HttpException).code())
    }
}
