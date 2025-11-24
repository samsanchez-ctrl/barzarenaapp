package cl.samuel.barzarena.data.repository

import cl.samuel.barzarena.data.local.dao.UserDao
import cl.samuel.barzarena.data.local.model.User
import cl.samuel.barzarena.data.remote.ApiService
import cl.samuel.barzarena.model.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService
) {

    suspend fun getRemoteData(): List<UserData> {
        return apiService.getDatos("obtenerDatos")
    }

    suspend fun registerUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    fun getUserById(id: Int): Flow<User?> {
        return userDao.getUserById(id)
    }

    fun getUserByUsername(username: String): Flow<User?> {
        return userDao.getUserByUsername(username)
    }

    fun getUserByEmail(email: String): Flow<User?> {
        return userDao.getUserByEmail(email)
    }

    fun getUserByPhone(phone: String): Flow<User?> {
        return userDao.getUserByPhone(phone)
    }

    fun getUserByRut(rut: String): Flow<User?> {
        return userDao.getUserByRut(rut)
    }

    suspend fun updateUserBalance(userId: Int, newBalance: Double) {
        userDao.updateUserBalance(userId, newBalance)
    }
}
