package cl.samuel.barzarena.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.samuel.barzarena.R
import cl.samuel.barzarena.model.Battle
import cl.samuel.barzarena.model.BetResult
import cl.samuel.barzarena.model.CartItem
import cl.samuel.barzarena.model.StoreItem
import cl.samuel.barzarena.model.UserData
import cl.samuel.barzarena.ui.auth.LoginScreen
import cl.samuel.barzarena.ui.auth.RegisterScreen
import cl.samuel.barzarena.viewmodel.MainViewModel
import java.util.Date
import kotlin.math.absoluteValue

// --- CONSTANTES Y ESTADO GLOBAL ---
enum class Screen { LOGIN, REGISTER, HOME, STORE, RECHARGE, BATTLES, HISTORY, CART }

// --- PANTALLA DE CARGA INICIAL ---
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Comprobando sesión...", fontSize = 18.sp, color = Color.Black)
        }
    }
}

// --- FUNCIÓN PRINCIPAL DE NAVEGACIÓN ---
@Composable
fun BarzarenaApp() {
    val vm: MainViewModel = hiltViewModel()
    val context = LocalContext.current

    // Primero, verifica si la sesión ha sido comprobada.
    if (!vm.isSessionChecked) {
        LoadingScreen()
        return // Muestra la pantalla de carga y no hace nada más hasta que la comprobación termine.
    }

    // Una vez comprobada la sesión, muestra la pantalla inicial y el resto de la navegación.
    var currentScreen by rememberSaveable {
        mutableStateOf(if (vm.isUserLoggedIn()) Screen.HOME else Screen.LOGIN)
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { user ->
                vm.loginSuccess(user.userName, user.balance.toInt(), user.id)
                currentScreen = Screen.HOME
            },
            onNavigateRegister = { currentScreen = Screen.REGISTER }
        )
        Screen.REGISTER -> RegisterScreen(
            onRegisterSuccess = { currentScreen = Screen.LOGIN },
            onNavigateLogin = { currentScreen = Screen.LOGIN }
        )
        Screen.HOME -> HomeScreen(
            username = vm.username,
            balance = vm.balance,
            remoteData = vm.remoteData,
            remoteDataError = vm.remoteDataError,
            onNavigate = { currentScreen = it },
            onLogout = {
                vm.logout()
                currentScreen = Screen.LOGIN
            },
        )
        Screen.STORE -> StoreScreen(
            balance = vm.balance,
            items = vm.storeItems.map {
                val imageResId = context.resources.getIdentifier(it.imageName, "drawable", context.packageName)
                StoreItem(it.name, it.price.toInt(), if (imageResId != 0) imageResId else R.drawable.dinero)
            },
            onAddToCart = { item ->
                vm.addToCart(item)
                Toast.makeText(context, "${item.name} agregado al carrito", Toast.LENGTH_SHORT).show()
            },
            onNavigateToCart = { currentScreen = Screen.CART },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.RECHARGE -> RechargeScreen(
            isRecharging = vm.isRecharging,
            onRecharge = { amount ->
                vm.rechargeBalance(amount) { success ->
                    if (success) {
                        Toast.makeText(context, "Recarga de $$amount OK", Toast.LENGTH_SHORT).show()
                        currentScreen = Screen.HOME
                    } else {
                        Toast.makeText(context, "Ingrese un monto válido mayor a cero.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.BATTLES -> BattlesScreen(
            balance = vm.balance,
            activeBattles = vm.activeBattles,
            isPlacingBet = vm.isPlacingBet,
            onPlaceBet = { battle, winner, amount ->
                vm.placeBet(battle, winner, amount) { result ->
                    val message = if (result == BetResult.WIN) {
                        "¡Ganaste! Recibes el doble."
                    } else {
                        "¡Perdiste! Suerte para la próxima."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    currentScreen = Screen.HOME
                }
            },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.HISTORY -> HistoryScreen(
            history = vm.betHistory,
            formatTimestamp = { vm.formatBetTimestamp(it) },
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.CART -> CartScreen(
            cartItems = vm.cartItems,
            onCheckout = {
                if (vm.checkout()) {
                    Toast.makeText(context, "¡Compra finalizada con éxito!", Toast.LENGTH_SHORT).show()
                    currentScreen = Screen.HOME
                } else {
                    Toast.makeText(context, "Saldo insuficiente para completar la compra.", Toast.LENGTH_SHORT).show()
                }
            },
            onRemoveFromCart = vm::removeFromCart,
            onBack = { currentScreen = Screen.STORE }
        )
        null -> {
            // Esto es necesario para que el `when` sea exhaustivo, aunque nunca debería llegar aquí.
            LoadingScreen()
        }
    }
}

// --- PANTALLA 3: HOME ---

@Composable
fun HomeScreen(
    username: String,
    balance: Int,
    remoteData: List<UserData>?,
    remoteDataError: String?,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
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

        // --- API Obtener Datos ---
        Text(
            text = "USUARIO RECIENTE Y RESULTADOS",
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        when {
            remoteDataError != null -> {
                Text(
                    text = "Error al cargar datos: $remoteDataError",
                    color = Color.Red
                )
            }
            remoteData == null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargando datos de la API...")
                }
            }
            remoteData.isEmpty() -> {
                Text("No se encontraron resultados de la API.")
            }
            else -> {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(remoteData) { data ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (data.usuario != null) {
                                    Text("Usuario: ${data.usuario}", fontWeight = FontWeight.Bold)
                                    Text("Correo: ${data.correo}")
                                } else {
                                    Text("Rapero: ${data.rapero}", fontWeight = FontWeight.Bold)
                                    Text("Monto Apostado: ${data.monto_apostado}")
                                    Text("Predicción: ${data.prediccion}")
                                    Text("Resultado: ${data.resultado}")
                                }
                            }
                        }
                    }
                }
            }
        }

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
fun StoreScreen(
    balance: Int,
    items: List<StoreItem>,
    onAddToCart: (StoreItem) -> Unit,
    onNavigateToCart: () -> Unit,
    onBack: () -> Unit
) {
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
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
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
                        onClick = { onAddToCart(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Agregar", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botón para ir al carrito
        Button(
            onClick = onNavigateToCart,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Ir al Carrito", color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

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
fun RechargeScreen(
    isRecharging: Boolean,
    onRecharge: (Int) -> Unit,
    onBack: () -> Unit
) {
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.dinero),
                contentDescription = "Recargar Saldo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp)
            )
            Text("RECARGAR SALDO", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    if (!isRecharging) {
                        amountText = newValue.filter { it.isDigit() }
                    }
                },
                label = { Text("Cantidad a recargar ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecharging
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
                enabled = amountInt > 0 && !isRecharging,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isRecharging) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("RECARGAR $ ${"%,d".format(amountInt)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Volver",
                color = Color.DarkGray,
                modifier = Modifier.clickable {
                    if (!isRecharging) {
                        onBack()
                    }
                }
            )
        }
    }
}


// --- PANTALLA 6: BATALLAS ACTIVAS Y APUESTAS ---
@Composable
fun BattlesScreen(
    balance: Int,
    activeBattles: List<Battle>,
    isPlacingBet: Boolean,
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
                        if (!isPlacingBet) {
                            selectedBattle = battle
                            selectedWinner = null // Reinicia la selección de ganador al elegir nueva batalla
                        }
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
                                Row(
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { if (!isPlacingBet) selectedWinner = battle.rapFighterA },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedWinner == battle.rapFighterA) Color.Black else Color.DarkGray),
                                        enabled = !isPlacingBet
                                    ) { Text(battle.rapFighterA, color = Color.White) }

                                    Button(
                                        onClick = { if (!isPlacingBet) selectedWinner = battle.rapFighterB },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedWinner == battle.rapFighterB) Color.Black else Color.DarkGray),
                                        enabled = !isPlacingBet
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
            HorizontalDivider(color = Color.LightGray)
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
                    .padding(vertical = 10.dp),
                enabled = !isPlacingBet
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
                },
                enabled = selectedWinner != null && betAmount > 0 && balance >= betAmount && !isPlacingBet,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isPlacingBet) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("APOSTAR $ ${"%,d".format(betAmount)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Volver",
            color = Color.DarkGray,
            modifier = Modifier.clickable { onBack() }.align(Alignment.CenterHorizontally)
        )
    }
}

// --- PANTALLA 7: HISTORIAL DE APUESTAS ---
@Composable
fun HistoryScreen(
    history: List<cl.samuel.barzarena.data.local.model.Bet>,
    formatTimestamp: (Date) -> String,
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
                    val isWin = bet.result == BetResult.WIN.name
                    val color = if (isWin) Color(0xFF006400) else Color.Red
                    val title = if (isWin) "APUESTA GANADA" else "APUESTA PERDIDA"
                    val winningsLabel = if (isWin) "Ganancia:" else "Pérdida:"
                    val winningsValue = bet.winnings.absoluteValue

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, color, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = if (isWin) R.drawable.dinero else R.drawable.fms_logo),
                                contentDescription = "Imagen de apuesta",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = color,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Batalla: ${bet.details}", color = Color.Black)
                                Text("Apostaste: $ ${"%,d".format(bet.amount.toInt())}", color = Color.DarkGray)
                                Text("$winningsLabel $ ${"%,d".format(winningsValue.toInt())}", color = color, fontWeight = FontWeight.SemiBold)
                                Text("Fecha: ${formatTimestamp(bet.date)}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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

// --- PANTALLA 8: CARRITO DE COMPRAS ---
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onCheckout: () -> Unit,
    onRemoveFromCart: (CartItem) -> Unit, // Añadi una función para eliminar
    onBack: () -> Unit
) {
    val totalCost = cartItems.sumOf { it.item.price * it.quantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text("CARRITO DE COMPRAS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu carrito está vacío.", color = Color.DarkGray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { cartItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${cartItem.quantity}x", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(cartItem.item.name, modifier = Modifier.weight(1f), fontSize = 16.sp, color = Color.Black)
                        Text("$ ${"%,d".format(cartItem.item.price * cartItem.quantity)}", fontSize = 16.sp, color = Color.DarkGray)
                        // Botón para eliminar el ítem
                        IconButton(onClick = { onRemoveFromCart(cartItem) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar ítem", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total:", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("$ ${"%,d".format(totalCost)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(20.dp))
            // Botón de finalizar compra
            Button(
                onClick = onCheckout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Finalizar Compra", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Volver a la tienda",
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
fun HomeScreenPreview() {
    HomeScreen(username = "Samuel", balance = 100000, remoteData = null, remoteDataError = null, onNavigate = {}, onLogout = {})
}
