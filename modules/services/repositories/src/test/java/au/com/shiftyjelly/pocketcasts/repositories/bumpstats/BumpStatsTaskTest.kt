package au.com.shiftyjelly.pocketcasts.repositories.bumpstats

import androidx.work.ListenableWorker.Result
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComServiceManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import retrofit2.Response

class BumpStatsTaskTest {
    private val bumpStatsDao = mock<BumpStatsDao>()
    private val appDatabase = mock<AppDatabase> {
        on { bumpStatsDao() } doReturn bumpStatsDao
    }
    private val wpComServiceManager = mock<WpComServiceManager>()

    @Test
    fun `removes invalid bump stats and uploads the valid ones`() = runTest {
        val valid = AnonymousBumpStat(name = "pcandroid_discover_list_impression_bump")
        val invalid = AnonymousBumpStat(name = "pcandroid_dıscover_lıst_ımpressıon_bump")
        givenStoredBumpStats(valid, invalid)
        givenAcceptedResponse()

        val result = BumpStatsTask.run(appDatabase, wpComServiceManager)

        assertEquals(Result.success(), result)
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(invalid)) }
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(valid)) }
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(valid)) }
    }

    @Test
    fun `only uploads valid bump stats when some are invalid`() = runTest {
        val valid = AnonymousBumpStat(name = "pcandroid_discover_list_impression_bump")
        val invalid = AnonymousBumpStat(name = "pcandroid_dıscover_lıst_ımpressıon_bump")
        givenStoredBumpStats(valid, invalid)
        givenAcceptedResponse()

        BumpStatsTask.run(appDatabase, wpComServiceManager)

        verifyBlocking(bumpStatsDao) { deleteAll(listOf(invalid)) }
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(valid)) }
    }

    @Test
    fun `succeeds without uploading when only invalid bump stats exist`() = runTest {
        val invalid = AnonymousBumpStat(name = "pcandroid_dıscover_lıst_ımpressıon_bump")
        givenStoredBumpStats(invalid)

        val result = BumpStatsTask.run(appDatabase, wpComServiceManager)

        assertEquals(Result.success(), result)
        verifyBlocking(bumpStatsDao) { deleteAll(listOf(invalid)) }
        verifyBlocking(wpComServiceManager, never()) { bumpStatAnonymously(any()) }
    }

    @Test
    fun `succeeds without uploading when no bump stats exist`() = runTest {
        givenStoredBumpStats()

        val result = BumpStatsTask.run(appDatabase, wpComServiceManager)

        assertEquals(Result.success(), result)
        verifyBlocking(wpComServiceManager, never()) { bumpStatAnonymously(any()) }
        verifyBlocking(bumpStatsDao, never()) { deleteAll(any()) }
    }

    @Test
    fun `keeps valid bump stats when the upload is rejected`() = runTest {
        val valid = AnonymousBumpStat(name = "pcandroid_discover_list_impression_bump")
        givenStoredBumpStats(valid)
        givenResponse(Response.success("Rejected"))

        val result = BumpStatsTask.run(appDatabase, wpComServiceManager)

        assertEquals(Result.failure(), result)
        verifyBlocking(wpComServiceManager) { bumpStatAnonymously(listOf(valid)) }
        verifyBlocking(bumpStatsDao, never()) { deleteAll(listOf(valid)) }
    }

    private fun givenStoredBumpStats(vararg bumpStats: AnonymousBumpStat) {
        whenever { bumpStatsDao.get() } doSuspendableAnswer { bumpStats.toList() }
    }

    private fun givenAcceptedResponse() {
        givenResponse(Response.success("Accepted"))
    }

    private fun givenResponse(response: Response<String>) {
        whenever { wpComServiceManager.bumpStatAnonymously(any()) } doSuspendableAnswer { response }
    }
}
