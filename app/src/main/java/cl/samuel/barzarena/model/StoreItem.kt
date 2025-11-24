package cl.samuel.barzarena.model

import androidx.annotation.DrawableRes

/**
 * Item de la tienda (Model)
 */
data class StoreItem(
    val name: String,
    val price: Int,
    @DrawableRes val imageResId: Int
)
