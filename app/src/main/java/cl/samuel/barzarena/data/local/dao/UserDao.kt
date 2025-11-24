package cl.samuel.barzarena.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.samuel.barzarena.data.local.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users WHERE userName = :username")
    fun getUserByUsername(username: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<User?>

    @Query("SELECT * FROM users WHERE phone = :phone")
    fun getUserByPhone(phone: String): Flow<User?>

    @Query("SELECT * FROM users WHERE rut = :rut")
    fun getUserByRut(rut: String): Flow<User?>

    @Query("UPDATE users SET balance = :newBalance WHERE id = :userId")
    suspend fun updateUserBalance(userId: Int, newBalance: Double)
}
