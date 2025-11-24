package cl.samuel.barzarena.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit
) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var rut by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") } // expecting dd/MM/yyyy

    val authState = authViewModel.authResult.collectAsState()

    LaunchedEffect(authState.value) {
        when (val event = authState.value) {
            is AuthEvent.RegistrationSuccess -> {
                Toast.makeText(context, "¡Cuenta creada! Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                onRegisterSuccess()
                authViewModel.resetAuthEvent()
            }
            is AuthEvent.Error -> {
                Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthEvent()
            }
            else -> Unit
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.DarkGray,
        cursorColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "CREAR CUENTA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 10.dp, bottom = 30.dp)
        )

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") }, colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña (mín. 6 caracteres)") }, visualTransformation = PasswordVisualTransformation(), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmar Contraseña") }, visualTransformation = PasswordVisualTransformation(), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = rut, onValueChange = { rut = it }, label = { Text("RUT") }, colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Fecha de Nacimiento (dd/MM/yyyy)") }, colors = textFieldColors, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (username.trim().isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.trim().isEmpty() || phone.trim().isEmpty() || rut.trim().isEmpty() || birthDate.trim().isEmpty()) {
                    Toast.makeText(context, "Todos los campos son obligatorios.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                
                authViewModel.registerUser(
                    userName = username.trim(),
                    password = password,
                    email = email.trim(),
                    phone = phone.trim(),
                    rut = rut.trim(),
                    birthDateStr = birthDate.trim()
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("REGISTRAR", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¿Ya tienes una cuenta? Inicia sesión aquí.",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.clickable { onNavigateLogin() }
        )
    }
}
