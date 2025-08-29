package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BlazeAdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(promotions: List<BlazeAd>)

    @Query("SELECT * FROM blaze_ads WHERE location = :location")
    abstract fun findByLocationFlow(location: BlazeAdLocation): Flow<List<BlazeAd>>

    @Query("DELETE FROM blaze_ads")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(blazeAds: List<BlazeAd>) {
        deleteAll()
        if (blazeAds.isNotEmpty()) {
            insertAll(blazeAds)
        }
    }
}
