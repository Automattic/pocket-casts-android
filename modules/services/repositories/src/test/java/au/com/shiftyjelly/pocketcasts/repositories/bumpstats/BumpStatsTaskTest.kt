package au.com.shiftyjelly.pocketcasts.repositories.bumpstats

import androidx.work.ListenableWorker.Result
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComServiceManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import retrofit2.Response

class BumpStatsTaskTest {
    private val bumpStatsDao = mock<BumpStatsDao>()
    private val appDatabase = mock<AppDatabase> {
        on { bumpStatsDao() } doReturn bumpStatsDao
    }
    private val wpComServiceManager = mock<WpComServiceManager>()
    private val clock = Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        whenever { bumpStatsDao.deleteOlderThan(any()) } doSuspendableAnswer { 0 }
        whenever { bumpStatsDao.deleteAllExceptNewest(any()) } doSuspendableAnswer { 0 }
    }

    @Test
    fun `removes invalid bump stats and uploads the valid ones`() = runTest {
        val valid = bumpStat(id = 1, name = "pcandroid_discover_list_impression_bump")
        val invalid = bumpStat(id = 2, name = "pcandroid_dıscover_lıst_ımpressıon_bump")
        givenStoredBatches(listOf(valid, invalid))
        givenAcceptedResponse()

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(invalid)) }
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(valid)) }
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(valid)) }
    }

    @Test
    fun `succeeds without uploading when only invalid bump stats exist`() = runTest {
        val invalid = bumpStat(id = 1, name = "pcandroid_dıscover_lıst_ımpressıon_bump")
        givenStoredBatches(listOf(invalid))

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(invalid)) }
        verifyBlocking(wpComServiceManager, never()) { bumpStatAnonymously(any()) }
    }

    @Test
    fun `succeeds without uploading when no bump stats exist`() = runTest {
        givenStoredBatches()

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(wpComServiceManager, never()) { bumpStatAnonymously(any()) }
        verifyBlocking(bumpStatsDao, never()) { deleteAll(any()) }
    }

    @Test
    fun `keeps valid bump stats when the upload is rejected`() = runTest {
        val valid = bumpStat(id = 1, name = "pcandroid_discover_list_impression_bump")
        givenStoredBatches(listOf(valid))
        givenResponse(Response.success("Rejected"))

        val result = runTask()

        assertEquals(Result.failure(), result)
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(valid)) }
        verifyBlocking(bumpStatsDao, never()) { deleteAll(any()) }
    }

    @Test
    fun `uploads each batch until none are left`() = runTest {
        val first = bumpStat(id = 1, name = "pcandroid_discover_list_impression_bump")
        val second = bumpStat(id = 2, name = "pcandroid_discover_list_podcast_tapped_bump")
        givenStoredBatches(listOf(first), listOf(second))
        givenAcceptedResponse()

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(first)) }
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(second)) }
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(first)) }
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(second)) }
    }

    @Test
    fun `stops after the maximum number of batches`() = runTest {
        val valid = bumpStat(id = 1, name = "pcandroid_discover_list_impression_bump")
        whenever { bumpStatsDao.getOldest(any()) } doSuspendableAnswer { listOf(valid) }
        givenAcceptedResponse()

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(bumpStatsDao, times(10)) { getOldest(500) }
    }

    @Test
    fun `prunes expired bump stats and keeps only the newest ones`() = runTest {
        givenStoredBatches()

        runTask()

        verifyBlocking(bumpStatsDao) { deleteOlderThan(clock.millis() - 30.days.inWholeMilliseconds) }
        verifyBlocking(bumpStatsDao) { deleteAllExceptNewest(5_000) }
    }

    private suspend fun runTask() = BumpStatsTask.run(appDatabase, wpComServiceManager, clock)

    private fun bumpStat(id: Long, name: String) = AnonymousBumpStat(name = name, eventTime = clock.millis(), id = id)

    private fun givenStoredBatches(vararg batches: List<AnonymousBumpStat>) {
        val remaining = ArrayDeque(batches.toList())
        whenever { bumpStatsDao.getOldest(any()) } doSuspendableAnswer { remaining.removeFirstOrNull().orEmpty() }
    }

    private fun givenAcceptedResponse() {
        givenResponse(Response.success("Accepted"))
    }

    private fun givenResponse(response: Response<String>) {
        whenever { wpComServiceManager.bumpStatAnonymously(any()) } doSuspendableAnswer { response }
    }
}
