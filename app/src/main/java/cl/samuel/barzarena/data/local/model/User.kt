package cl.samuel.barzarena.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userName: String,
    val password: String,
    val email: String,
    val phone: String,
    val rut: String,
    val birthDate: Date,
    val balance: Double
)
