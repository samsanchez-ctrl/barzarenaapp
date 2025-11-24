package cl.samuel.barzarena.model

import androidx.annotation.DrawableRes
import cl.samuel.barzarena.R

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

// Las batallas activas predeterminadas (simulación)
val ActiveBattles = listOf(
    Battle(1, "Trueno", "Dani", "Trueno", R.drawable.trueno),
    Battle(2, "Wos", "Mks", "Wos", R.drawable.wos),
    Battle(3, "Aczino", "Gazir", "Gazir", R.drawable.gazir)
)
