package cl.samuel.barzarena.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.samuel.barzarena.data.local.model.User
import cl.samuel.barzarena.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authResult = MutableStateFlow<AuthEvent>(AuthEvent.Idle)
    val authResult: StateFlow<AuthEvent> = _authResult.asStateFlow()

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun registerUser(
        userName: String,
        password: String,
        email: String,
        phone: String,
        rut: String,
        birthDateStr: String
    ) {
        viewModelScope.launch {
            // 1. Validaciones de Unicidad
            if (userRepository.getUserByUsername(userName).firstOrNull() != null) {
                _authResult.value = AuthEvent.Error("El nombre de usuario ya existe.")
                return@launch
            }
            if (userRepository.getUserByEmail(email).firstOrNull() != null) {
                _authResult.value = AuthEvent.Error("El correo electrónico ya está registrado.")
                return@launch
            }
            if (userRepository.getUserByPhone(phone).firstOrNull() != null) {
                _authResult.value = AuthEvent.Error("El teléfono ya está registrado.")
                return@launch
            }
            if (userRepository.getUserByRut(rut).firstOrNull() != null) {
                _authResult.value = AuthEvent.Error("El RUT ya está registrado.")
                return@launch
            }

            // 2. Validación de formato de RUT
            if (!isValidChileanRut(rut)) {
                _authResult.value = AuthEvent.Error("El formato del RUT no es válido.")
                return@launch
            }

            // 3. Validación de Fecha de Nacimiento y Edad
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate: Date = try {
                sdf.parse(birthDateStr) ?: run {
                    _authResult.value = AuthEvent.Error("Formato de fecha inválido. Use dd/MM/yyyy.")
                    return@launch
                }
            } catch (e: Exception) {
                _authResult.value = AuthEvent.Error("Formato de fecha inválido. Use dd/MM/yyyy.")
                return@launch
            }

            val today = Calendar.getInstance()
            val birth = Calendar.getInstance()
            birth.time = birthDate

            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            if (age < 18) {
                _authResult.value = AuthEvent.Error("Debes ser mayor de 18 años para registrarte.")
                return@launch
            }

            // 4. Hashear la contraseña y crear el usuario
            val hashedPassword = hashPassword(password)
            val newUser = User(
                userName = userName,
                password = hashedPassword, // Guardar la contraseña hasheada
                email = email,
                phone = phone,
                rut = rut,
                birthDate = birthDate,
                balance = 1000.0 // Saldo inicial
            )
            userRepository.registerUser(newUser)
            _authResult.value = AuthEvent.RegistrationSuccess
        }
    }

    private fun isValidChileanRut(rut: String): Boolean {
        val cleanRut = rut.replace(Regex("[.-]"), "").uppercase()
        if (!cleanRut.matches(Regex("^\\d{7,8}[0-9K]$"))) return false

        val body = cleanRut.substring(0, cleanRut.length - 1)
        val dv = cleanRut.last()

        return try {
            var sum = 0
            var multiple = 2
            for (i in body.reversed()) {
                sum += Character.getNumericValue(i) * multiple
                multiple = if (multiple == 7) 2 else multiple + 1
            }
            val expectedDv = when (val mod = 11 - (sum % 11)) {
                11 -> '0'
                10 -> 'K'
                else -> Character.forDigit(mod, 10)
            }
            dv == expectedDv
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(userName: String, password: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByUsername(userName).firstOrNull()
            val hashedPassword = hashPassword(password)

            if (user != null && user.password == hashedPassword) {
                _authResult.value = AuthEvent.LoginSuccess(user)
            } else {
                _authResult.value = AuthEvent.Error("Usuario o contraseña incorrectos.")
            }
        }
    }

    fun resetAuthEvent() {
        _authResult.value = AuthEvent.Idle
    }
}

sealed class AuthEvent {
    data class LoginSuccess(val user: User) : AuthEvent()
    object RegistrationSuccess : AuthEvent()
    data class Error(val message: String) : AuthEvent()
    object Idle : AuthEvent()
}
