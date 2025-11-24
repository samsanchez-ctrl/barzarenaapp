package cl.samuel.barzarena.model

/**
 * Representa un item en el carrito de compras.
 */
data class CartItem(
    val item: StoreItem,
    var quantity: Int = 1
)
