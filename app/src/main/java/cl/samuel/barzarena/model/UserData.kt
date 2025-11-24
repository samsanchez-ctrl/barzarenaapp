package cl.samuel.barzarena.model

import java.util.UUID

data class UserData(
    val usuario: String?,
    val correo: String?,
    val rapero: String?,
    val monto_apostado: Int?,
    val prediccion: String?,
    val resultado: String?
)

/**
 * Representa una apuesta realizada por el usuario.
 */
data class Bet(
    val id: String = UUID.randomUUID().toString(),
    val battle: String,
    val winnerSelected: String,
    val amount: Int,
    val result: BetResult,
    val finalWinnings: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BetResult { WIN, LOSS, PENDING }
