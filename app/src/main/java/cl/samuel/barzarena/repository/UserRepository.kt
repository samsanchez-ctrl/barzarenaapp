package cl.samuel.barzarena.repository

import android.content.Context
import cl.samuel.barzarena.data.remote.RetrofitInstance
import cl.samuel.barzarena.model.Bet
import cl.samuel.barzarena.model.UserData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class UserRepository(context: Context) {

    private val PREFS_NAME = "BarzarenaPrefs"
    private val BALANCE_KEY = "user_balance"
    private val USERNAME_KEY = "user_username"
    private val HISTORY_KEY = "bet_history"
    private val INITIAL_BALANCE = 50000

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // API Service
    private val apiService = RetrofitInstance.api

    fun getUsername(): String? = prefs.getString(USERNAME_KEY, null)
    fun getBalance(): Int = prefs.getInt(BALANCE_KEY, INITIAL_BALANCE)

    fun saveSession(username: String) {
        prefs.edit().putString(USERNAME_KEY, username).apply()
    }

    fun saveBalance(balance: Int) {
        prefs.edit().putInt(BALANCE_KEY, balance).apply()
    }

    fun clearSession() {
        prefs.edit().remove(USERNAME_KEY).apply()
    }

    // --- Remote Data ---
    suspend fun getRemoteData(): List<UserData>? {
        return try {
            apiService.getDatos()
        } catch (e: IOException) {

            e.printStackTrace()
            null
        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    // --- Historial de Apuestas ---

    fun getBetHistory(): List<Bet> {
        val json = prefs.getString(HISTORY_KEY, null)
        return if (json == null) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Bet>>() {}.type
            gson.fromJson(json, type)
        }
    }

    fun saveBet(newBet: Bet) {
        val currentHistory = getBetHistory().toMutableList()
        currentHistory.add(0, newBet)

        val json = gson.toJson(currentHistory)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
}
