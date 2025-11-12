package cl.samuel.barzarena.model

import androidx.annotation.DrawableRes
import cl.samuel.barzarena.R
import java.util.UUID

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

/**
 * Datos para las Batallas Activas (simulación)
 */
data class Battle(
    val id: Int,
    val rapFighterA: String,
    val rapFighterB: String,
    val predictedWinner: String, // El ganador predeterminado (secreto)
    @DrawableRes val imageResId: Int
)

/**
 * Item de la tienda (Model)
 */
data class StoreItem(
    val name: String,
    val price: Int,
    val imageUrl: String? = null
)

// Las batallas activas predeterminadas (simulación)
val ActiveBattles = listOf(
    Battle(1, "Trueno", "Dani", "Trueno", R.drawable.trueno),
    Battle(2, "Wos", "Mks", "Wos", R.drawable.wos),
    Battle(3, "Aczino", "Gazir", "Gazir", R.drawable.gazir)
)
