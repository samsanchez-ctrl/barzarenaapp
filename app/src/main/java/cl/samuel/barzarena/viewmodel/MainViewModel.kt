package cl.samuel.barzarena.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.samuel.barzarena.R
import cl.samuel.barzarena.model.ActiveBattles
import cl.samuel.barzarena.model.Battle
import cl.samuel.barzarena.model.Bet
import cl.samuel.barzarena.model.BetResult
import cl.samuel.barzarena.model.CartItem
import cl.samuel.barzarena.model.StoreItem
import cl.samuel.barzarena.model.UserData
import cl.samuel.barzarena.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: UserRepository) : ViewModel() {

    var username by mutableStateOf(repository.getUsername() ?: "")
        private set
    var balance by mutableStateOf(repository.getBalance())
        private set
    var betHistory by mutableStateOf(repository.getBetHistory())
        private set

    var remoteData by mutableStateOf<List<UserData>?>(null)
        private set

    // Tienda
    val storeItems = listOf(
        StoreItem("Micrófono de Oro", 15000, R.drawable.microfonodeoro),
        StoreItem("Cadena de Lujo", 5000, R.drawable.cadenadelujo),
        StoreItem("Pulsera de Lujo", 25000, R.drawable.pulseradelujo)
    )

    // Carrito
    var cartItems = mutableStateListOf<CartItem>()
        private set

    // Batallas
    val activeBattles: List<Battle> = ActiveBattles

    init {
        fetchRemoteData()
    }

    // --- SESIÓN Y SALDO ---

    fun loginSuccess(user: String) {
        username = user
        balance = repository.getBalance() // Carga el saldo guardado
        repository.saveSession(user) // Guarda solo la sesión del usuario
        loadHistory()
    }

    fun logout() {
        repository.clearSession()
        username = ""
        balance = repository.getBalance() // Vuelve al saldo inicial
        betHistory = emptyList()
    }

    /**
     * Actualiza el saldo y lo guarda en el repositorio.
     */
    fun updateBalance(amount: Int) {
        val newBalance = balance + amount
        balance = newBalance
        repository.saveBalance(newBalance) // Guarda el nuevo saldo
    }

    // --- RECARGA VARIABLE ---

    fun rechargeBalance(amount: Int): Boolean {
        if (amount <= 0) return false
        updateBalance(amount)
        return true
    }

    // --- APUESTAS ---

    fun placeBet(battle: Battle, winnerSelected: String, amount: Int): Bet {
        val result = if (winnerSelected == battle.predictedWinner) BetResult.WIN else BetResult.LOSS
        val finalWinnings = if (result == BetResult.WIN) amount * 2 else -amount

        // 1. Descontar/Aumentar Saldo
        updateBalance(finalWinnings)

        // 2. Registrar Apuesta
        val newBet = Bet(
            battle = "${battle.rapFighterA} vs ${battle.rapFighterB}",
            winnerSelected = winnerSelected,
            amount = amount,
            result = result,
            finalWinnings = finalWinnings
        )
        repository.saveBet(newBet)
        loadHistory()

        return newBet
    }

    // --- HISTORIAL ---

    private fun loadHistory() {
        betHistory = repository.getBetHistory()
    }

    fun formatBetTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // --- REMOTE DATA ---
    private fun fetchRemoteData() {
        viewModelScope.launch {
            remoteData = repository.getRemoteData()
        }
    }

    // --- CARRITO ---

    fun addToCart(item: StoreItem) {
        val existingItem = cartItems.find { it.item.name == item.name }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            cartItems.add(CartItem(item))
        }
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun checkout(): Boolean {
        val totalCost = cartItems.sumOf { it.item.price * it.quantity }
        if (balance >= totalCost) {
            updateBalance(-totalCost)
            clearCart()
            return true
        }
        return false
    }
}
