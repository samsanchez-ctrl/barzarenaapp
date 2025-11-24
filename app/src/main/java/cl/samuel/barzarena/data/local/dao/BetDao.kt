package cl.samuel.barzarena.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.samuel.barzarena.data.local.model.Bet
import kotlinx.coroutines.flow.Flow

@Dao
interface BetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBet(bet: Bet)

    @Update
    suspend fun updateBet(bet: Bet)

    @Delete
    suspend fun deleteBet(bet: Bet)

    @Query("SELECT * FROM bets WHERE userId = :userId ORDER BY date DESC")
    fun getBetHistoryForUser(userId: Int): Flow<List<Bet>>
}
