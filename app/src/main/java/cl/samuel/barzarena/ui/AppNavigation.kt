package cl.samuel.barzarena.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.samuel.barzarena.R
import cl.samuel.barzarena.model.Battle
import cl.samuel.barzarena.model.Bet
import cl.samuel.barzarena.model.BetResult
import cl.samuel.barzarena.model.StoreItem
import cl.samuel.barzarena.viewmodel.MainViewModel
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

// --- CONSTANTES Y ESTADO GLOBAL ---
enum class Screen { LOGIN, REGISTER, HOME, STORE, RECHARGE, BATTLES, HISTORY }

// --- FUNCIÓN PRINCIPAL DE NAVEGACIÓN ---

@Composable
fun BarzarenaApp() {
    // Inyección del ViewModel usando Hilt
    val vm: MainViewModel = hiltViewModel()
    val context = LocalContext.current

    // Estado de la Sesión y Navegación
    var currentScreen by rememberSaveable {
        mutableStateOf(if (vm.username.isNotEmpty()) Screen.HOME else Screen.LOGIN)
    }

    // Dibujar la pantalla actual
    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { username ->
                vm.loginSuccess(username)
                currentScreen = Screen.HOME
            },
            onNavigateRegister = { currentScreen = Screen.REGISTER }
        )
        Screen.REGISTER -> RegisterScreen(
            onRegisterSuccess = { currentScreen = Screen.LOGIN }
        )
        Screen.HOME -> HomeScreen(
            username = vm.username,
            balance = vm.balance,
            onNavigate = { currentScreen = it },
            onLogout = {
                vm.logout()
                currentScreen = Screen.LOGIN // Asegura la redirección inmediata a LOGIN
            },
        )
        Screen.STORE -> StoreScreen(
            balance = vm.balance,
            items = vm.storeItems,
            onPurchase = { price ->
                vm.updateBalance(-price)
                Toast.makeText(context, "¡Compra realizada!", Toast.LENGTH_SHORT).show()
            },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.RECHARGE -> RechargeScreen(
            onRecharge = { amount ->
                if (vm.rechargeBalance(amount)) {
                    Toast.makeText(context, "Recarga de $$amount OK", Toast.LENGTH_SHORT).show()
                    currentScreen = Screen.HOME
                } else {
                    Toast.makeText(context, "Ingrese un monto válido mayor a cero.", Toast.LENGTH_SHORT).show()
                }
            },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.BATTLES -> BattlesScreen(
            balance = vm.balance,
            activeBattles = vm.activeBattles,
            onPlaceBet = { battle, winner, amount ->
                val betResult = vm.placeBet(battle, winner, amount)
                val msg = if (betResult.result == BetResult.WIN) "¡GANASTE! +$${betResult.finalWinnings}" else "PERDISTE. -$${betResult.amount}"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                currentScreen = Screen.HOME // Regresa a Home después de la apuesta
            },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.HISTORY -> HistoryScreen(
            history = vm.betHistory,
            formatTimestamp = { vm.formatBetTimestamp(it) },
            onBack = { currentScreen = Screen.HOME }
        )
    }
}

// UTILIDADES DE VALIDACIÓN

/**
 * Verifica si la fecha de nacimiento (YYYY-MM-DD) corresponde a alguien mayor de 18 años.
 */
fun isOlderThan18(dobString: String): Boolean {
    if (dobString.length != 10) return false
    val parts = dobString.split('-').mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return false
    val (year, month, day) = parts
    val dob = Calendar.getInstance().apply { set(year, month - 1, day) }
    val today = Calendar.getInstance()
    val eighteenYearsAgo = today.clone() as Calendar
    eighteenYearsAgo.add(Calendar.YEAR, -18)
    return dob.before(eighteenYearsAgo) || dob.equals(eighteenYearsAgo)
}

/**
 * Valida un RUT chileno (ficticio, sin usar puntos) con guion y dígito verificador.
 */
fun isValidRUT(rutInput: String): Boolean {
    val rut = rutInput.replace(".", "").replace(" ", "").uppercase(Locale.getDefault())
    if (!rut.contains("-")) return false
    val parts = rut.split("-")
    if (parts.size != 2) return false
    val number = parts[0]
    val dv = parts[1]
    if (!Pattern.matches("\\d+", number)) return false

    var sum = 0
    var multiplier = 2
    for (i in number.length - 1 downTo 0) {
        sum += number[i].digitToInt() * multiplier
        multiplier++
        if (multiplier == 8) multiplier = 2
    }
    val rest = 11 - (sum % 11)
    val dvCalculated = when (rest) {
        11 -> "0"
        10 -> "K"
        else -> rest.toString()
    }
    return dvCalculated == dv
}


// --- PANTALLA 1: LOGIN ---

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onNavigateRegister: () -> Unit) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BARZARENA",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 60.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de usuario", color = Color.DarkGray) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña", color = Color.DarkGray) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (username.trim().isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Debe ingresar usuario y contraseña.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (username.trim().lowercase(Locale.ROOT) == "samuel" && password == "password123") {
                    onLoginSuccess(username.trim())
                } else {
                    Toast.makeText(context, "ERROR: Usuario o contraseña incorrectos.", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("INICIAR SESIÓN", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¿No tienes cuenta? Regístrate aquí.",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.clickable { onNavigateRegister() }
        )
    }
}

// --- PANTALLA 2: REGISTER ---

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var rut by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }

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
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña (mín. 6)") }, visualTransformation = PasswordVisualTransformation(), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmar") }, visualTransformation = PasswordVisualTransformation(), colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = rut, onValueChange = { rut = it }, label = { Text("RUT (Ej: 12345678-K)") }, colors = textFieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it.take(10) },
            label = { Text("Fecha Nacimiento (AAAA-MM-DD)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (username.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || rut.trim().isEmpty() || dob.trim().isEmpty()) {
                    Toast.makeText(context, "ERROR: Todos los campos son obligatorios.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    Toast.makeText(context, "ERROR: Email inválido.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "ERROR: La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "ERROR: Las contraseñas no coinciden.", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (!isValidRUT(rut.trim())) {
                    Toast.makeText(context, "ERROR: RUT inválido. Debe ser sin puntos y con guion (ej: 12345678-K).", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (!isOlderThan18(dob.trim())) {
                    Toast.makeText(context, "ERROR: Debes ser mayor de 18 años para apostar.", Toast.LENGTH_LONG).show()
                    return@Button
                }

                Toast.makeText(context, "Cuenta creada exitosamente. Ve a iniciar sesión por favor.", Toast.LENGTH_LONG).show()
                onRegisterSuccess()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("REGISTRAR", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- PANTALLA 3: HOME ---

@Composable
fun HomeScreen(
    username: String,
    balance: Int,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // --- Logo FMS ---
        Image(
            painter = painterResource(id = R.drawable.fms_logo),
            contentDescription = "FMS Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(bottom = 20.dp)
                .align(Alignment.CenterHorizontally)
        )

        // --- Sección de Saldo ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(20.dp)
                .height(IntrinsicSize.Min)
        ) {
            Text(
                text = "¡Bienvenido, $username!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SALDO DISPONIBLE",
                color = Color.DarkGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "$ ${"%,d".format(balance)}",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { onNavigate(Screen.RECHARGE) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.padding(top = 15.dp)
            ) {
                Text("RECARGAR SALDO", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- Opciones del Menú Principal ---
        Text(
            text = "OPCIONES",
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        // Botón Apostar
        MenuButton(text = "APOSTAR EN BATALLAS") { onNavigate(Screen.BATTLES) }

        // Botón Tienda
        MenuButton(text = "TIENDA (COMPRAR ÍTEMS)", onClick = { onNavigate(Screen.STORE) })

        // Botón Historial
        MenuButton(text = "HISTORIAL DE APUESTAS") { onNavigate(Screen.HISTORY) }


        Spacer(modifier = Modifier.height(20.dp))

        // Botón Cerrar Sesión
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF808080)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("CERRAR SESIÓN", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// --- PANTALLA 4: TIENDA ---
@Composable
fun StoreScreen(balance: Int, items: List<StoreItem>, onPurchase: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIENDA URBANA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Saldo: $ ${"%,d".format(balance)}",
                fontSize = 16.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lista de ítems
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("$ ${"%,d".format(item.price)}", color = Color.DarkGray)
                }
                Button(
                    onClick = {
                        if (balance >= item.price) {
                            onPurchase(item.price)
                        } else {
                            Toast.makeText(context, "Saldo insuficiente para ${item.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    enabled = balance >= item.price
                ) {
                    Text("Comprar", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Volver al Inicio", color = Color.White)
        }
    }
}

// --- PANTALLA 5: RECARGA VARIABLE ---
@Composable
fun RechargeScreen(onRecharge: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var amountText by rememberSaveable { mutableStateOf("") }
    val amountInt = amountText.toIntOrNull() ?: 0

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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("RECARGAR SALDO", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                amountText = newValue.filter { it.isDigit() }
            },
            label = { Text("Cantidad a recargar ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (amountInt > 0) {
                    onRecharge(amountInt)
                } else {
                    Toast.makeText(context, "Ingrese una cantidad válida.", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = amountInt > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("RECARGAR $ ${"%,d".format(amountInt)}", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Volver",
            color = Color.DarkGray,
            modifier = Modifier.clickable { onBack() }
        )
    }
}

// --- PANTALLA 6: BATALLAS ACTIVAS Y APUESTAS ---
@Composable
fun BattlesScreen(
    balance: Int,
    activeBattles: List<Battle>,
    onPlaceBet: (Battle, String, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedBattle by rememberSaveable { mutableStateOf<Battle?>(null) }
    var selectedWinner by rememberSaveable { mutableStateOf<String?>(null) }
    var betAmountText by rememberSaveable { mutableStateOf("5000") }
    val betAmount = betAmountText.toIntOrNull() ?: 0

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
            .padding(24.dp)
    ) {
        Text("BATALLAS ACTIVAS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Saldo: $ ${"%,d".format(balance)}", fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(20.dp))

        // Lista de Batallas
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(activeBattles) { battle ->
                val isSelected = selectedBattle?.id == battle.id
                Card(
                    onClick = {
                        selectedBattle = battle
                        selectedWinner = null // Reinicia la selección de ganador al elegir nueva batalla
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSelected) Color.Black else Color.LightGray, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEEEEEE) else Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = battle.imageResId),
                            contentDescription = "Imagen de la batalla",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("${battle.rapFighterA} vs ${battle.rapFighterB}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 20.sp)
                            Text("Elige quien crees que gane", color = Color.DarkGray, fontSize = 14.sp)

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(10.dp))
                                // Botones para elegir ganador
                                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                                    // Botón para Fighter A
                                    Button(
                                        onClick = { selectedWinner = battle.rapFighterA },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedWinner == battle.rapFighterA) Color.Black else Color.DarkGray)
                                    ) { Text(battle.rapFighterA, color = Color.White) }

                                    // Botón para Fighter B
                                    Button(
                                        onClick = { selectedWinner = battle.rapFighterB },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedWinner == battle.rapFighterB) Color.Black else Color.DarkGray)
                                    ) { Text(battle.rapFighterB, color = Color.White) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Panel de Apuesta (Fijo en la parte inferior) ---
        if (selectedBattle != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.LightGray)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Apostando en: ${selectedBattle!!.rapFighterA} vs ${selectedBattle!!.rapFighterB}", fontWeight = FontWeight.Bold)
            Text("Tu Selección: ${selectedWinner ?: "Selecciona un ganador"}", color = if (selectedWinner == null) Color.Red else Color.Black)

            OutlinedTextField(
                value = betAmountText,
                onValueChange = { betAmountText = it.filter { char -> char.isDigit() } },
                label = { Text("Monto a Apostar ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )

            Button(
                onClick = {
                    if (selectedWinner == null) {
                        Toast.makeText(context, "Debe seleccionar un ganador.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (betAmount <= 0) {
                        Toast.makeText(context, "Ingrese un monto válido.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (balance < betAmount) {
                        Toast.makeText(context, "Saldo insuficiente.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    onPlaceBet(selectedBattle!!, selectedWinner!!, betAmount)
                    selectedBattle = null
                    selectedWinner = null
                },
                enabled = selectedWinner != null && betAmount > 0 && balance >= betAmount,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("APOSTAR $ ${"%,d".format(betAmount)}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Volver",
            color = Color.DarkGray,
            modifier = Modifier
                .clickable { onBack() }
                .align(Alignment.CenterHorizontally)
        )
    }
}

// --- PANTALLA 7: HISTORIAL DE APUESTAS ---
@Composable
fun HistoryScreen(
    history: List<Bet>,
    formatTimestamp: (Long) -> String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text("HISTORIAL DE APUESTAS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        if (history.isEmpty()) {
            Text("Aún no has realizado ninguna apuesta.", color = Color.DarkGray)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(history) { bet ->
                    val color = when (bet.result) {
                        BetResult.WIN -> Color(0xFF006400) // Verde Oscuro
                        BetResult.LOSS -> Color(0xFFB22222) // Rojo Ladrillo
                        BetResult.PENDING -> Color.Gray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, color, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.dinero),
                                contentDescription = "Imagen de apuesta",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                // Línea 1: Resultado
                                Text(
                                    text = when (bet.result) {
                                        BetResult.WIN -> "¡GANADOR!"
                                        BetResult.LOSS -> "PERDEDOR"
                                        else -> "PENDIENTE"
                                    },
                                    color = color,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Línea 2: Batalla y Apuesta
                                Text("Batalla: ${bet.battle}", color = Color.Black)
                                Text("Apostaste: $ ${"%,d".format(bet.amount)} por ${bet.winnerSelected}", color = Color.DarkGray)

                                // Línea 3: Ganancia/Pérdida
                                val winText = if (bet.finalWinnings >= 0) "Ganancia: +$ ${"%,d".format(bet.finalWinnings)}" else "Pérdida: -$ ${"%,d".format(-bet.finalWinnings)}"
                                Text(winText, color = color)

                                // Línea 4: Fecha
                                Text("Fecha: ${formatTimestamp(bet.timestamp)}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Volver",
            color = Color.DarkGray,
            modifier = Modifier
                .clickable { onBack() }
                .align(Alignment.CenterHorizontally)
        )
    }
}

// --- COMPONENTE REUTILIZABLE PARA BOTONES DE MENÚ ---
@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(bottom = 8.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {}, onNavigateRegister = {})
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(username = "Samuel", balance = 100000, onNavigate = {}, onLogout = {})
}
