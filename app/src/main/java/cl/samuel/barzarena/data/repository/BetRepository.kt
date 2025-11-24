package cl.samuel.barzarena.data.repository

import cl.samuel.barzarena.data.local.dao.BetDao
import cl.samuel.barzarena.data.local.model.Bet
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BetRepository @Inject constructor(
    private val betDao: BetDao
) {

    suspend fun placeBet(bet: Bet) {
        betDao.insertBet(bet)
    }

    suspend fun updateBet(bet: Bet) {
        betDao.updateBet(bet)
    }

    suspend fun deleteBet(bet: Bet) {
        betDao.deleteBet(bet)
    }

    fun getBetHistory(userId: Int): Flow<List<Bet>> {
        return betDao.getBetHistoryForUser(userId)
    }
}
