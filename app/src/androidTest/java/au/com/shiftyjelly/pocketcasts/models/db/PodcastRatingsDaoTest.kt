package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastRatingsDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastRatingsDaoTest {
    private lateinit var ratingsDao: PodcastRatingsDao
    private lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        ratingsDao = testDb.podcastRatingsDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun updateRatings() = runTest {
        val ratings = listOf(
            UserPodcastRating("id-1", rating = 1, modifiedAt = Date(0)),
            UserPodcastRating("id-2", rating = 3, modifiedAt = Date(1)),
        )

        ratingsDao.updateUserRatings(ratings)
        val result = ratingsDao.getAllUserRatings()

        assertEquals(ratings, result)
    }

    @Test
    fun updateOnlyMissingOrNewerRatings() = runTest {
        ratingsDao.insertOrReplaceUserRatings(
            listOf(
                UserPodcastRating("id-1", rating = 1, modifiedAt = Date(10)),
                UserPodcastRating("id-2", rating = 1, modifiedAt = Date(10)),
            ),
        )

        ratingsDao.updateUserRatings(
            listOf(
                UserPodcastRating("id-1", rating = 2, modifiedAt = Date(9)),
                UserPodcastRating("id-2", rating = 2, modifiedAt = Date(10)),
                UserPodcastRating("id-3", rating = 2, modifiedAt = Date(0)),
            ),
        )

        val result = ratingsDao.getAllUserRatings()

        val expected = listOf(
            UserPodcastRating("id-1", rating = 1, modifiedAt = Date(10)),
            UserPodcastRating("id-2", rating = 2, modifiedAt = Date(10)),
            UserPodcastRating("id-3", rating = 2, modifiedAt = Date(0)),
        )
        assertEquals(expected, result)
    }
}
