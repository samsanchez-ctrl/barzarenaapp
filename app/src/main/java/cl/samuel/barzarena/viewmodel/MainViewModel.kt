package cl.samuel.barzarena.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.samuel.barzarena.data.local.SessionManager
import cl.samuel.barzarena.data.local.model.Bet
import cl.samuel.barzarena.data.local.model.Item
import cl.samuel.barzarena.data.repository.BetRepository
import cl.samuel.barzarena.data.repository.ItemRepository
import cl.samuel.barzarena.data.repository.UserRepository
import cl.samuel.barzarena.model.ActiveBattles
import cl.samuel.barzarena.model.Battle
import cl.samuel.barzarena.model.BetResult
import cl.samuel.barzarena.model.CartItem
import cl.samuel.barzarena.model.StoreItem
import cl.samuel.barzarena.model.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val betRepository: BetRepository,
    private val itemRepository: ItemRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var username by mutableStateOf("")
        private set
    var balance by mutableIntStateOf(0)
        private set
    var userId by mutableIntStateOf(0)
        private set

    var isSessionChecked by mutableStateOf(false)
        private set

    var isRecharging by mutableStateOf(false)
        private set

    var isPlacingBet by mutableStateOf(false)
        private set

    var betHistory = mutableStateListOf<Bet>()
        private set

    var remoteData by mutableStateOf<List<UserData>?>(null)
        private set

    var remoteDataError by mutableStateOf<String?>(null)
        private set

    val storeItems = mutableStateListOf<Item>()

    var cartItems = mutableStateListOf<CartItem>()
        private set

    val activeBattles: List<Battle> = ActiveBattles

    init {
        checkForActiveSession()
    }

    private fun checkForActiveSession() {
        viewModelScope.launch {
            val loggedInUserId = sessionManager.getUserId()
            if (loggedInUserId != null) {
                val user = userRepository.getUserById(loggedInUserId).first()
                if (user != null) {
                    loginSuccess(user.userName, user.balance.toInt(), user.id)
                }
            }
            isSessionChecked = true // Marcamos la sesión como verificada
        }
    }

    fun isUserLoggedIn(): Boolean {
        return username.isNotEmpty()
    }

    fun loginSuccess(loggedInUsername: String, loggedInBalance: Int, loggedInUserId: Int) {
        username = loggedInUsername
        balance = loggedInBalance
        userId = loggedInUserId
        remoteDataError = null
        sessionManager.saveSession(loggedInUserId) // Guardamos la sesión
        loadInitialData()
    }

    fun logout() {
        sessionManager.clearSession() // Limpiamos la sesión
        username = ""
        balance = 0
        userId = 0
        betHistory.clear()
        storeItems.clear()
        // Ya no limpiamos el carrito al cerrar sesión
        remoteData = null
        remoteDataError = null
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            betRepository.getBetHistory(userId).collect { history ->
                betHistory.clear()
                betHistory.addAll(history)
            }
        }
        viewModelScope.launch {
            itemRepository.getStoreItems().collect { items ->
                storeItems.clear()
                storeItems.addAll(items)
            }
        }
        viewModelScope.launch {
            try {
                remoteData = userRepository.getRemoteData()
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error de red desconocido"
                Log.e("MainViewModel", "Error fetching remote data: $errorMessage")
                remoteDataError = "Error de red: $errorMessage"
                remoteData = emptyList() // Asignar lista vacía para detener la carga
            }
        }
    }

    fun rechargeBalance(amount: Int, onResult: (Boolean) -> Unit) {
        if (amount <= 0) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            isRecharging = true
            delay(2500) // Simulación del procesamiento de pago
            val newBalance = balance + amount
            userRepository.updateUserBalance(userId, newBalance.toDouble())
            balance = newBalance
            isRecharging = false
            onResult(true)
        }
    }

    fun placeBet(battle: Battle, winnerSelected: String, amount: Int, onBetResult: (BetResult) -> Unit) {
        viewModelScope.launch {
            isPlacingBet = true
            delay(2500) // Simulación del procesamiento de la apuesta

            val result = if (winnerSelected == battle.predictedWinner) BetResult.WIN else BetResult.LOSS
            val finalWinnings = if (result == BetResult.WIN) amount * 2.0 else -amount.toDouble()

            val newBalance = balance + finalWinnings

            userRepository.updateUserBalance(userId, newBalance)
            balance = newBalance.toInt()

            val newBet = Bet(
                userId = userId,
                amount = amount.toDouble(),
                details = "${battle.rapFighterA} vs ${battle.rapFighterB}",
                date = Date(),
                result = result.name, // Guardar "WIN" o "LOSS"
                winnings = finalWinnings
            )
            betRepository.placeBet(newBet)

            isPlacingBet = false
            onBetResult(result)
        }
    }

    fun formatBetTimestamp(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    fun addToCart(item: StoreItem) {
        val existingItem = cartItems.find { it.item.name == item.name }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            cartItems.add(CartItem(item))
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        cartItems.remove(cartItem)
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun checkout(): Boolean {
        val totalCost = cartItems.sumOf { it.item.price * it.quantity }
        if (balance >= totalCost) {
            val newBalance = balance - totalCost
            viewModelScope.launch {
                userRepository.updateUserBalance(userId, newBalance.toDouble())
                balance = newBalance
                // Aquí también deberías actualizar el stock de los items en la BD
            }
            clearCart()
            return true
        }
        return false
    }
}
