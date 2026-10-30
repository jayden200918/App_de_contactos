package com.example.app_de_contactos

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DarkBg = Color(0xFF09111E)
val CardBg = Color(0xFF101B2B)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val AppBlue = Color(0xFF2563EB)
val AppLightBlue = Color(0xFF38BDF8)
val WhatsAppGreen = AppLightBlue
val ChipSelectedBg = Color(0xFF172E48)
val BottomBarBg = Color(0xFF070E18)
val BorderDark = Color(0xFF1E2D42)
val ErrorRed = Color(0xFFFF5252)

data class CategoriaNota(
    val nombre: String,
    val color: Color,
    val hex: String
)

val categoriasDisponibles = listOf(
    CategoriaNota("Personal", Color(0xFF38BDF8), "#38BDF8"),
    CategoriaNota("Trabajo", Color(0xFF2563EB), "#2563EB"),
    CategoriaNota("Importante", Color(0xFF818CF8), "#818CF8"),
    CategoriaNota("Idea", Color(0xFF06B6D4), "#06B6D4")
)

fun parseHexColor(hex: String, fallback: Color = AppLightBlue): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

data class Nota(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val categoria: String = "Personal",
    val colorHex: String = "#38BDF8"
)

data class Contacto(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val detalle: String = "",
    val fechaAgregado: String = ""
)

data class MensajeChat(
    val id: String = "",
    val emisorTelefono: String = "",
    val emisorNombre: String = "",
    val texto: String = "",
    val fechaHora: String = ""
)

data class LlamadaRegistro(
    val id: String = "",
    val nombreContacto: String = "Desconocido",
    val telefono: String = "",
    val fechaHora: String = "",
    val tipo: String = "saliente"
)

data class DialButtonInfo(val number: String, val letters: String = "")

val dialPadMatrix = listOf(
    listOf(DialButtonInfo("1", ""), DialButtonInfo("2", "ABC"), DialButtonInfo("3", "DEF")),
    listOf(DialButtonInfo("4", "GHI"), DialButtonInfo("5", "JKL"), DialButtonInfo("6", "MNO")),
    listOf(DialButtonInfo("7", "PQRS"), DialButtonInfo("8", "TUV"), DialButtonInfo("9", "WXYZ")),
    listOf(DialButtonInfo("*", ""), DialButtonInfo("0", "+"), DialButtonInfo("#", ""))
)

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_NAME = "user_name"

    fun saveSession(context: Context, phone: String, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_PHONE, phone)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, "") ?: ""
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        probarConexiones()

        setContent {
            val context = LocalContext.current
            var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn(context)) }

            ContactosAppTheme {
                Crossfade(
                    targetState = isLoggedIn,
                    animationSpec = tween(durationMillis = 350),
                    label = "SessionStateTransition"
                ) { loggedIn ->
                    if (!loggedIn) {
                        LoginScreen(
                            onLoginSuccess = { phone, name ->
                                SessionManager.saveSession(context, phone, name)
                                isLoggedIn = true
                            }
                        )
                    } else {
                        MainScreen(
                            onLogout = {
                                SessionManager.clearSession(context)
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (_: Exception) {}
                                isLoggedIn = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun probarConexiones() {
        try {
            val app = FirebaseApp.getInstance()
            Log.d("AppCheck", "Servicio conectado: ${app.name}")
        } catch (e: Exception) {
            Log.e("AppCheck", "Error de conexión", e)
        }
    }
}

@Composable
fun ContactosAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = AppBlue,
            secondary = AppLightBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun LoginScreen(onLoginSuccess: (phone: String, name: String) -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .background(Color(0xFF060D17)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "App contacto",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRegisterMode) "Crea tu cuenta" else "Bienvenido de nuevo",
                    fontSize = 14.sp,
                    color = AppLightBlue
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) "REGISTRARSE" else "INICIAR SESIÓN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nombre de usuario", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Nombre de usuario",
                            tint = AppLightBlue
                        )
                    },
                    singleLine = true,
                    isError = (errorMessage != null && username.isBlank()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        errorBorderColor = ErrorRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input.filter { char -> char.isDigit() || char == '+' }
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Número de teléfono",
                            tint = AppLightBlue
                        )
                    },
                    singleLine = true,
                    isError = (errorMessage != null && phone.isBlank()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        errorBorderColor = ErrorRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (successMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = successMessage!!,
                        color = AppLightBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val cleanUser = username.trim()
                        val cleanPhone = phone.trim()

                        if (cleanUser.isBlank() || cleanPhone.isBlank()) {
                            errorMessage = "Por favor, debes rellenar todos los espacios."
                            return@Button
                        }

                        errorMessage = null
                        successMessage = null
                        isLoading = true

                        val db = FirebaseFirestore.getInstance()
                        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")

                        val userMap = hashMapOf(
                            "nombre" to cleanUser,
                            "telefono" to cleanPhone
                        )

                        if (isRegisterMode) {
                            rtdb.getReference("usuarios").child(cleanPhone).setValue(userMap)
                            try {
                                db.collection("usuarios").document(cleanPhone).set(userMap)
                            } catch (_: Exception) {}

                            isLoading = false
                            onLoginSuccess(cleanPhone, cleanUser)
                        } else {
                            rtdb.getReference("usuarios").child(cleanPhone).get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.exists()) {
                                        val nombreGuardado = snapshot.child("nombre").getValue(String::class.java) ?: ""
                                        if (nombreGuardado.equals(cleanUser, ignoreCase = true)) {
                                            isLoading = false
                                            onLoginSuccess(cleanPhone, cleanUser)
                                        } else {
                                            isLoading = false
                                            errorMessage = "El nombre de usuario no coincide con el número ingresado."
                                        }
                                    } else {
                                        db.collection("usuarios").document(cleanPhone).get()
                                            .addOnSuccessListener { document ->
                                                isLoading = false
                                                if (document.exists()) {
                                                    val nombreGuardado = document.getString("nombre") ?: ""
                                                    if (nombreGuardado.equals(cleanUser, ignoreCase = true)) {
                                                        onLoginSuccess(cleanPhone, cleanUser)
                                                    } else {
                                                        errorMessage = "El nombre de usuario no coincide con el número ingresado."
                                                    }
                                                } else {
                                                    errorMessage = "Usuario no registrado. Por favor, haz clic en REGÍSTRATE."
                                                }
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                errorMessage = "Usuario no registrado. Por favor, haz clic en REGÍSTRATE."
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    db.collection("usuarios").document(cleanPhone).get()
                                        .addOnSuccessListener { document ->
                                            isLoading = false
                                            if (document.exists()) {
                                                val nombreGuardado = document.getString("nombre") ?: ""
                                                if (nombreGuardado.equals(cleanUser, ignoreCase = true)) {
                                                    onLoginSuccess(cleanPhone, cleanUser)
                                                } else {
                                                    errorMessage = "El nombre de usuario no coincide con el número ingresado."
                                                }
                                            } else {
                                                errorMessage = "Usuario no registrado. Por favor, haz clic en REGÍSTRATE."
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Error de conexión: ${e.localizedMessage}"
                                        }
                                }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "REGISTRARSE" else "INICIAR SESIÓN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isRegisterMode) "¿YA TIENES CUENTA?\nINICIA SESIÓN" else "¿NO TIENES CUENTA?\nREGÍSTRATE",
                    color = AppLightBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable {
                            isRegisterMode = !isRegisterMode
                            errorMessage = null
                            successMessage = null
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val userName = remember { SessionManager.getUserName(context) }
    val userPhone = remember { SessionManager.getUserPhone(context) }

    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            userName = userName,
            userPhone = userPhone,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                onLogout()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App contacto",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Perfil",
                                tint = AppLightBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(CardBg)
                        ) {
                            if (userName.isNotBlank() || userPhone.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(AppBlue, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                if (userName.isNotBlank()) {
                                                    Text(
                                                        text = userName,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                if (userPhone.isNotBlank()) {
                                                    Text(
                                                        text = userPhone,
                                                        color = TextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                                HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Cerrar sesión",
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                        contentDescription = "Cerrar sesión",
                                        tint = ErrorRed
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showLogoutDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BottomBarBg,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ContactPage,
                            contentDescription = "Contactos"
                        )
                    },
                    label = {
                        Text(
                            text = "Contactos",
                            fontWeight = if (selectedBottomTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppLightBlue,
                        selectedTextColor = TextPrimary,
                        indicatorColor = ChipSelectedBg,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Notas"
                        )
                    },
                    label = {
                        Text(
                            text = "Notas",
                            fontWeight = if (selectedBottomTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppLightBlue,
                        selectedTextColor = TextPrimary,
                        indicatorColor = ChipSelectedBg,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Llamadas"
                        )
                    },
                    label = {
                        Text(
                            text = "Llamadas",
                            fontWeight = if (selectedBottomTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppLightBlue,
                        selectedTextColor = TextPrimary,
                        indicatorColor = ChipSelectedBg,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedBottomTab) {
                0 -> ContactosContent(userPhone = userPhone)
                1 -> NotasContent(userPhone = userPhone)
                2 -> LlamadasContent(userPhone = userPhone)
            }
        }
    }
}

@Composable
fun LogoutConfirmationDialog(
    userName: String,
    userPhone: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isLoggingOut by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoggingOut) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF3B181E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "¿Cerrar sesión?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (userName.isNotBlank() || userPhone.isNotBlank()) {
                    Surface(
                        color = Color(0xFF162338),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(AppBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                if (userName.isNotBlank()) {
                                    Text(
                                        text = userName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                if (userPhone.isNotBlank()) {
                                    Text(
                                        text = userPhone,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¿Estás seguro de que deseas salir de tu cuenta?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoggingOut = true
                    onConfirm()
                },
                enabled = !isLoggingOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Cerrar sesión", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoggingOut,
                border = BorderStroke(1.dp, BorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
fun ContactosContent(userPhone: String) {
    val contactos = remember { mutableStateListOf<Contacto>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var contactoToEdit by remember { mutableStateOf<Contacto?>(null) }
    var contactoToDelete by remember { mutableStateOf<Contacto?>(null) }
    var selectedContactoForDetail by remember { mutableStateOf<Contacto?>(null) }
    var contactoForChat by remember { mutableStateOf<Contacto?>(null) }

    val cleanPhone = userPhone.trim()
    val context = LocalContext.current

    DisposableEffect(cleanPhone) {
        if (cleanPhone.isBlank()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
        val ref = rtdb.getReference("usuarios").child(cleanPhone).child("contactos")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Contacto>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    val telefono = child.child("telefono").getValue(String::class.java) ?: ""
                    val detalle = child.child("detalle").getValue(String::class.java) ?: ""
                    val fechaAgregado = child.child("fechaAgregado").getValue(String::class.java) ?: ""

                    if (id.isNotBlank()) {
                        list.add(Contacto(id = id, nombre = nombre, telefono = telefono, detalle = detalle, fechaAgregado = fechaAgregado))
                    }
                }
                contactos.clear()
                contactos.addAll(list.sortedBy { it.nombre.lowercase() })
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }

        ref.addValueEventListener(listener)

        onDispose {
            ref.removeEventListener(listener)
        }
    }

    if (contactoForChat != null) {
        val myName = SessionManager.getUserName(context)
        PantallaChatDialog(
            miTelefono = cleanPhone,
            miNombre = if (myName.isNotBlank()) myName else "Yo",
            contactoNombre = contactoForChat!!.nombre,
            contactoTelefono = contactoForChat!!.telefono,
            onDismiss = { contactoForChat = null }
        )
    }

    if (showAddDialog) {
        AgregarContactoDialog(
            onDismiss = { showAddDialog = false },
            onSaveInApp = { nombre, telefono, detalle ->
                showAddDialog = false
                guardarContactoEnBD(cleanPhone, nombre, telefono, detalle)
            },
            onSaveToDevice = { nombre, telefono, detalle ->
                showAddDialog = false
                guardarContactoEnBD(cleanPhone, nombre, telefono, detalle)
                guardarEnAgendaDelDispositivo(context, nombre, telefono)
            }
        )
    }

    if (selectedContactoForDetail != null) {
        VerDetalleContactoDialog(
            contacto = selectedContactoForDetail!!,
            onDismiss = { selectedContactoForDetail = null },
            onEdit = {
                val target = selectedContactoForDetail
                selectedContactoForDetail = null
                contactoToEdit = target
            },
            onDelete = {
                val target = selectedContactoForDetail
                selectedContactoForDetail = null
                contactoToDelete = target
            },
            onCall = {
                val tel = selectedContactoForDetail?.telefono ?: ""
                val nom = selectedContactoForDetail?.nombre ?: "Desconocido"
                selectedContactoForDetail = null
                if (tel.isNotBlank()) {
                    realizarYLlamadaYRegistrar(context, cleanPhone, tel, nom)
                }
            },
            onChat = {
                val target = selectedContactoForDetail
                selectedContactoForDetail = null
                if (target != null) {
                    contactoForChat = target
                }
            }
        )
    }

    if (contactoToEdit != null) {
        EditarContactoDialog(
            contacto = contactoToEdit!!,
            onDismiss = { contactoToEdit = null },
            onSave = { nuevoNombre, nuevoTelefono, nuevoDetalle ->
                val targetId = contactoToEdit?.id ?: ""
                contactoToEdit = null
                if (targetId.isNotBlank()) {
                    actualizarContactoEnBD(cleanPhone, targetId, nuevoNombre, nuevoTelefono, nuevoDetalle)
                }
            }
        )
    }

    if (contactoToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactoToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardBg,
            title = {
                Text("¿Eliminar contacto?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar a ${contactoToDelete?.nombre}?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = contactoToDelete
                        contactoToDelete = null
                        if (target != null) {
                            eliminarContactoDeBD(cleanPhone, target.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { contactoToDelete = null },
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            }
        )
    }

    val contactosFiltrados = remember(contactos, searchQuery) {
        if (searchQuery.isBlank()) {
            contactos
        } else {
            contactos.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.telefono.contains(searchQuery, ignoreCase = true) ||
                it.detalle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (contactos.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar contacto o detalle...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = AppLightBlue
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppLightBlue)
                }
            } else if (contactos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContactPage,
                        contentDescription = null,
                        tint = AppLightBlue,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes contactos guardados",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Agrega a tus amigos y familiares presionando el botón '+'.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (contactosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron contactos para \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(contactosFiltrados, key = { it.id }) { contacto ->
                        ContactoItemCard(
                            contacto = contacto,
                            onCardClick = { selectedContactoForDetail = contacto },
                            onEditClick = { contactoToEdit = contacto },
                            onDeleteClick = { contactoToDelete = contacto },
                            onCallClick = {
                                realizarYLlamadaYRegistrar(context, cleanPhone, contacto.telefono, contacto.nombre)
                            },
                            onChatClick = {
                                contactoForChat = contacto
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = AppBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar contacto",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ContactoItemCard(
    contacto: Contacto,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppBlue.copy(alpha = 0.25f), CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (contacto.nombre.isNotBlank()) contacto.nombre.take(1).uppercase() else "C",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppLightBlue
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = contacto.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contacto.telefono,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    if (contacto.detalle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = AppLightBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = contacto.detalle,
                                fontSize = 12.sp,
                                color = AppLightBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = "Llamar a ${contacto.nombre}",
                        tint = AppLightBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "Chat con ${contacto.nombre}",
                        tint = AppLightBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar contacto",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar contacto",
                        tint = ErrorRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerDetalleContactoDialog(
    contacto: Contacto,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onChat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(AppBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (contacto.nombre.isNotBlank()) contacto.nombre.take(1).uppercase() else "C",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Detalle del Contacto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color(0xFF162338),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = contacto.nombre,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = AppLightBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = contacto.telefono,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        if (contacto.detalle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderDark)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Detalle / Nota:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contacto.detalle,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }

                        if (contacto.fechaAgregado.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Agregado el ${contacto.fechaAgregado}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onChat,
                        border = BorderStroke(1.dp, AppLightBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppLightBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = null,
                            tint = AppLightBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat App", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Llamar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        border = BorderStroke(1.dp, ErrorRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar", color = ErrorRed, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        border = BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun PantallaChatDialog(
    miTelefono: String,
    miNombre: String,
    contactoNombre: String,
    contactoTelefono: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var textoMensaje by remember { mutableStateOf("") }
    val mensajes = remember { mutableStateListOf<MensajeChat>() }
    var isLoadingMensajes by remember { mutableStateOf(true) }

    val cleanMyPhone = miTelefono.trim()
    val cleanContactPhone = contactoTelefono.trim()

    val chatRoomId = remember(cleanMyPhone, cleanContactPhone) {
        if (cleanMyPhone < cleanContactPhone) {
            "${cleanMyPhone}_${cleanContactPhone}"
        } else {
            "${cleanContactPhone}_${cleanMyPhone}"
        }
    }

    DisposableEffect(chatRoomId) {
        if (chatRoomId.isBlank()) {
            isLoadingMensajes = false
            return@DisposableEffect onDispose {}
        }

        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
        val ref = rtdb.getReference("chats").child(chatRoomId).child("mensajes")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MensajeChat>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val emisor = child.child("emisorTelefono").getValue(String::class.java) ?: ""
                    val emisorNom = child.child("emisorNombre").getValue(String::class.java) ?: ""
                    val txt = child.child("texto").getValue(String::class.java) ?: ""
                    val fecha = child.child("fechaHora").getValue(String::class.java) ?: ""

                    if (id.isNotBlank() && txt.isNotBlank()) {
                        list.add(
                            MensajeChat(
                                id = id,
                                emisorTelefono = emisor,
                                emisorNombre = emisorNom,
                                texto = txt,
                                fechaHora = fecha
                            )
                        )
                    }
                }
                mensajes.clear()
                mensajes.addAll(list)
                isLoadingMensajes = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingMensajes = false
            }
        }

        ref.addValueEventListener(listener)

        onDispose {
            ref.removeEventListener(listener)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.98f)
            .fillMaxHeight(0.92f),
        shape = RoundedCornerShape(24.dp),
        containerColor = DarkBg,
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(AppBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (contactoNombre.isNotBlank()) contactoNombre.take(1).uppercase() else "C",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (contactoNombre.isNotBlank()) contactoNombre else contactoTelefono,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "En línea",
                            fontSize = 12.sp,
                            color = AppLightBlue
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            enviarMensajeSMS(context, contactoTelefono, "")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "SMS",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalDivider(color = BorderDark)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (isLoadingMensajes) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppLightBlue)
                        }
                    } else if (mensajes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = null,
                                tint = AppLightBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Comienza la conversación",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Envía un mensaje para chatear en tiempo real con ${if (contactoNombre.isNotBlank()) contactoNombre else contactoTelefono}.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(mensajes, key = { it.id }) { msg ->
                                val esMio = msg.emisorTelefono == cleanMyPhone
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (esMio) Color(0xFF1E3A5F) else CardBg,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (esMio) 16.dp else 2.dp,
                                            bottomEnd = if (esMio) 2.dp else 16.dp
                                        ),
                                        border = BorderStroke(1.dp, if (esMio) AppBlue.copy(alpha = 0.5f) else BorderDark),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = msg.texto,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.fechaHora,
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        placeholder = { Text("Escribe un mensaje...", color = TextSecondary, fontSize = 13.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppLightBlue,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val cleanTxt = textoMensaje.trim()
                            if (cleanTxt.isNotBlank()) {
                                enviarMensajeChat(
                                    chatRoomId = chatRoomId,
                                    emisorPhone = cleanMyPhone,
                                    emisorNombre = miNombre,
                                    texto = cleanTxt
                                )
                                textoMensaje = ""
                            }
                        },
                        enabled = textoMensaje.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (textoMensaje.isNotBlank()) AppBlue else Color(0xFF162338),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Enviar mensaje",
                            tint = if (textoMensaje.isNotBlank()) Color.White else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun enviarMensajeChat(
    chatRoomId: String,
    emisorPhone: String,
    emisorNombre: String,
    texto: String
) {
    if (chatRoomId.isBlank() || emisorPhone.isBlank() || texto.isBlank()) return

    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val messagesRef = rtdb.getReference("chats").child(chatRoomId).child("mensajes")
    val msgId = messagesRef.push().key ?: System.currentTimeMillis().toString()

    val msgMap = hashMapOf(
        "id" to msgId,
        "emisorTelefono" to emisorPhone,
        "emisorNombre" to emisorNombre,
        "texto" to texto,
        "fechaHora" to dateStr
    )

    messagesRef.child(msgId).setValue(msgMap)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatRoomId)
                    .collection("mensajes")
                    .document(msgId)
                    .set(msgMap)
            } catch (_: Exception) {}
        }
}

private fun enviarMensajeSMS(context: Context, telefono: String, mensaje: String) {
    val cleanTel = telefono.trim()
    if (cleanTel.isBlank()) return

    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:$cleanTel")
            putExtra("sms_body", mensaje)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("SMS", "Error al abrir SMS", e)
    }
}

@Composable
fun AgregarContactoDialog(
    onDismiss: () -> Unit,
    onSaveInApp: (nombre: String, telefono: String, detalle: String) -> Unit,
    onSaveToDevice: (nombre: String, telefono: String, detalle: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AppBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = AppLightBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Nuevo Contacto",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nombre del contacto", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = AppLightBlue
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { input ->
                        telefono = input.filter { char -> char.isDigit() || char == '+' }
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = AppLightBlue
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = detalle,
                    onValueChange = {
                        detalle = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Detalle o Nota (Opcional)", color = TextSecondary) },
                    placeholder = { Text("Ej: Amigo de trabajo, Familiar...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = AppLightBlue
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val cleanNombre = nombre.trim()
                        val cleanTelefono = telefono.trim()
                        val cleanDetalle = detalle.trim()

                        if (cleanNombre.isBlank() || cleanTelefono.isBlank()) {
                            errorMessage = "Por favor, completa el nombre y el número."
                            return@Button
                        }

                        onSaveInApp(cleanNombre, cleanTelefono, cleanDetalle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Guardar en la App", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val cleanNombre = nombre.trim()
                        val cleanTelefono = telefono.trim()
                        val cleanDetalle = detalle.trim()

                        if (cleanNombre.isBlank() || cleanTelefono.isBlank()) {
                            errorMessage = "Por favor, completa el nombre y el número."
                            return@OutlinedButton
                        }

                        onSaveToDevice(cleanNombre, cleanTelefono, cleanDetalle)
                    },
                    border = BorderStroke(1.dp, AppLightBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppLightBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar en Agenda del Celular", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            }
        }
    )
}

@Composable
fun EditarContactoDialog(
    contacto: Contacto,
    onDismiss: () -> Unit,
    onSave: (nombre: String, telefono: String, detalle: String) -> Unit
) {
    var nombre by remember { mutableStateOf(contacto.nombre) }
    var telefono by remember { mutableStateOf(contacto.telefono) }
    var detalle by remember { mutableStateOf(contacto.detalle) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editar Contacto",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nombre del contacto", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { input ->
                        telefono = input.filter { char -> char.isDigit() || char == '+' }
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = detalle,
                    onValueChange = {
                        detalle = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Detalle o Nota", color = TextSecondary) },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLightBlue,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanNombre = nombre.trim()
                    val cleanTelefono = telefono.trim()
                    val cleanDetalle = detalle.trim()

                    if (cleanNombre.isBlank() || cleanTelefono.isBlank()) {
                        errorMessage = "Por favor, completa todos los campos principales."
                        return@Button
                    }

                    onSave(cleanNombre, cleanTelefono, cleanDetalle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar", color = TextPrimary)
            }
        }
    )
}

@Composable
fun NotasContent(userPhone: String) {
    val notas = remember { mutableStateListOf<Nota>() }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var notaToDelete by remember { mutableStateOf<Nota?>(null) }
    var selectedNotaForDetail by remember { mutableStateOf<Nota?>(null) }

    val cleanPhone = userPhone.trim()

    DisposableEffect(cleanPhone) {
        if (cleanPhone.isBlank()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
        val ref = rtdb.getReference("usuarios").child(cleanPhone).child("notas")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Nota>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val titulo = child.child("titulo").getValue(String::class.java) ?: ""
                    val descripcion = child.child("descripcion").getValue(String::class.java) ?: ""
                    val fecha = child.child("fecha").getValue(String::class.java) ?: ""
                    val categoria = child.child("categoria").getValue(String::class.java) ?: "Personal"
                    val colorHex = child.child("colorHex").getValue(String::class.java) ?: "#38BDF8"

                    if (id.isNotBlank()) {
                        list.add(
                            Nota(
                                id = id,
                                titulo = titulo,
                                descripcion = descripcion,
                                fecha = fecha,
                                categoria = categoria,
                                colorHex = colorHex
                            )
                        )
                    }
                }
                notas.clear()
                notas.addAll(list.reversed())
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }

        ref.addValueEventListener(listener)

        onDispose {
            ref.removeEventListener(listener)
        }
    }

    if (showAddDialog) {
        AgregarNotaDialog(
            onDismiss = { showAddDialog = false },
            onSave = { titulo, descripcion, categoria, colorHex ->
                showAddDialog = false
                guardarNotaEnBD(cleanPhone, titulo, descripcion, categoria, colorHex)
            }
        )
    }

    if (notaToDelete != null) {
        AlertDialog(
            onDismissRequest = { notaToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardBg,
            title = {
                Text("¿Eliminar nota?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Esta nota se eliminará permanentemente.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = notaToDelete
                        notaToDelete = null
                        if (target != null) {
                            eliminarNotaDeBD(cleanPhone, target.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { notaToDelete = null },
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            }
        )
    }

    if (selectedNotaForDetail != null) {
        VerDetalleNotaDialog(
            nota = selectedNotaForDetail!!,
            userPhone = cleanPhone,
            onDismiss = { selectedNotaForDetail = null },
            onDelete = {
                val target = selectedNotaForDetail
                selectedNotaForDetail = null
                if (target != null) {
                    notaToDelete = target
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppLightBlue)
            }
        } else if (notas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = AppLightBlue,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No tienes notas creadas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Presiona el botón '+' para crear tu primera nota organizada por categorías.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notas, key = { it.id }) { nota ->
                    NotaItemCard(
                        nota = nota,
                        onCardClick = { selectedNotaForDetail = nota },
                        onDeleteClick = { notaToDelete = nota }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = AppBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar nota",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun NotaItemCard(
    nota: Nota,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val tagColor = parseHexColor(nota.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(tagColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = tagColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, tagColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = nota.categoria,
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onCardClick() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar nota",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Eliminar nota",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = nota.titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (nota.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nota.descripcion,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }

                if (nota.fecha.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = nota.fecha,
                        fontSize = 11.sp,
                        color = Color(0xFF667781),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerDetalleNotaDialog(
    nota: Nota,
    userPhone: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var editTitulo by remember { mutableStateOf(nota.titulo) }
    var editDescripcion by remember { mutableStateOf(nota.descripcion) }
    var editCategoria by remember { mutableStateOf(categoriasDisponibles.find { it.nombre == nota.categoria } ?: categoriasDisponibles.first()) }
    var isSavingChanges by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentTagColor = parseHexColor(if (isEditMode) editCategoria.hex else nota.colorHex)

    AlertDialog(
        onDismissRequest = { if (!isSavingChanges) onDismiss() },
        modifier = Modifier.fillMaxWidth(0.95f),
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = currentTagColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, currentTagColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(currentTagColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) editCategoria.nombre else nota.categoria,
                            color = currentTagColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!isEditMode) {
                    Text(
                        text = if (nota.titulo.isBlank()) "Sin título" else nota.titulo,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (nota.fecha.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AppLightBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Creada: ${nota.fecha}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFF162338),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (nota.descripcion.isBlank()) "(Sin descripción ni contenido adicional)" else nota.descripcion,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Categoría:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoriasDisponibles) { cat ->
                            val isSelected = cat.nombre == editCategoria.nombre
                            Surface(
                                onClick = { editCategoria = cat },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) cat.color.copy(alpha = 0.25f) else Color(0xFF162338),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) cat.color else BorderDark
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(cat.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.nombre,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editTitulo,
                        onValueChange = {
                            editTitulo = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text("Título de la nota", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = editCategoria.color,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editDescripcion,
                        onValueChange = {
                            editDescripcion = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text("Descripción o contenido", color = TextSecondary) },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = editCategoria.color,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isEditMode) {
                Button(
                    onClick = { isEditMode = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        val cleanTitle = editTitulo.trim()
                        val cleanDesc = editDescripcion.trim()

                        if (cleanTitle.isBlank() && cleanDesc.isBlank()) {
                            errorMessage = "Ingresa al menos un título o una descripción."
                            return@Button
                        }

                        isSavingChanges = true
                        actualizarNotaEnBD(
                            phone = userPhone,
                            noteId = nota.id,
                            titulo = cleanTitle,
                            descripcion = cleanDesc,
                            categoria = editCategoria.nombre,
                            colorHex = editCategoria.hex
                        )
                        isSavingChanges = false
                        onDismiss()
                    },
                    enabled = !isSavingChanges,
                    colors = ButtonDefaults.buttonColors(containerColor = editCategoria.color),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSavingChanges) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isEditMode) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    border = BorderStroke(1.dp, ErrorRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar")
                }
            } else {
                OutlinedButton(
                    onClick = { isEditMode = false },
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            }
        }
    )
}

@Composable
fun AgregarNotaDialog(
    onDismiss: () -> Unit,
    onSave: (titulo: String, descripcion: String, categoria: String, colorHex: String) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoriasDisponibles.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val maxTitleLength = 50
    val maxDescLength = 500

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AppBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = AppLightBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Nueva Nota",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Categoría:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriasDisponibles) { cat ->
                        val isSelected = cat.nombre == selectedCategory.nombre
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) cat.color.copy(alpha = 0.25f) else Color(0xFF162338),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) cat.color else BorderDark
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(cat.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.nombre,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = cat.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        if (it.length <= maxTitleLength) {
                            titulo = it
                            if (errorMessage != null) errorMessage = null
                        }
                    },
                    label = { Text("Título de la nota", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Title,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "${titulo.length}/$maxTitleLength",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedCategory.color,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        if (it.length <= maxDescLength) {
                            descripcion = it
                            if (errorMessage != null) errorMessage = null
                        }
                    },
                    label = { Text("Descripción o contenido", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "${descripcion.length}/$maxDescLength",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedCategory.color,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = titulo.trim()
                    val cleanDesc = descripcion.trim()

                    if (cleanTitle.isBlank() && cleanDesc.isBlank()) {
                        errorMessage = "Por favor, ingresa al menos un título o una descripción."
                        return@Button
                    }

                    onSave(cleanTitle, cleanDesc, selectedCategory.nombre, selectedCategory.hex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = selectedCategory.color),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Guardar Nota", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar", color = TextPrimary)
            }
        }
    )
}

private fun guardarNotaEnBD(
    phone: String,
    titulo: String,
    descripcion: String,
    categoria: String,
    colorHex: String
) {
    if (phone.isBlank()) return

    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val notesRef = rtdb.getReference("usuarios").child(phone).child("notas")
    val noteId = notesRef.push().key ?: System.currentTimeMillis().toString()

    val noteMap = hashMapOf(
        "id" to noteId,
        "titulo" to titulo,
        "descripcion" to descripcion,
        "fecha" to dateStr,
        "categoria" to categoria,
        "colorHex" to colorHex
    )

    notesRef.child(noteId).setValue(noteMap)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(phone)
                    .collection("notas")
                    .document(noteId)
                    .set(noteMap)
            } catch (_: Exception) {}
        }
}

private fun actualizarNotaEnBD(
    phone: String,
    noteId: String,
    titulo: String,
    descripcion: String,
    categoria: String,
    colorHex: String
) {
    if (phone.isBlank() || noteId.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val updates = hashMapOf<String, Any>(
        "titulo" to titulo,
        "descripcion" to descripcion,
        "categoria" to categoria,
        "colorHex" to colorHex
    )

    rtdb.getReference("usuarios").child(phone).child("notas").child(noteId).updateChildren(updates)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(phone)
                    .collection("notas")
                    .document(noteId)
                    .update(updates)
            } catch (_: Exception) {}
        }
}

private fun eliminarNotaDeBD(phone: String, noteId: String) {
    if (phone.isBlank() || noteId.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    rtdb.getReference("usuarios").child(phone).child("notas").child(noteId).removeValue()
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(phone)
                    .collection("notas")
                    .document(noteId)
                    .delete()
            } catch (_: Exception) {}
        }
}

private fun realizarYLlamadaYRegistrar(
    context: Context,
    userPhone: String,
    telefonoDestino: String,
    nombreContacto: String
) {
    val cleanTel = telefonoDestino.trim()
    if (cleanTel.isBlank()) return

    registrarLlamadaEnBD(userPhone, cleanTel, nombreContacto)

    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_DIAL,
            android.net.Uri.parse("tel:$cleanTel")
        )
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("Dialer", "Error al abrir marcador", e)
    }
}

private fun registrarLlamadaEnBD(
    userPhone: String,
    telefonoDestino: String,
    nombreContacto: String
) {
    if (userPhone.isBlank() || telefonoDestino.isBlank()) return

    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val callsRef = rtdb.getReference("usuarios").child(userPhone).child("llamadas")
    val callId = callsRef.push().key ?: System.currentTimeMillis().toString()

    val callMap = hashMapOf(
        "id" to callId,
        "nombreContacto" to nombreContacto,
        "telefono" to telefonoDestino,
        "fechaHora" to dateStr,
        "tipo" to "saliente"
    )

    callsRef.child(callId).setValue(callMap)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userPhone)
                    .collection("llamadas")
                    .document(callId)
                    .set(callMap)
            } catch (_: Exception) {}
        }
}

private fun eliminarLlamadaDeBD(userPhone: String, callId: String) {
    if (userPhone.isBlank() || callId.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    rtdb.getReference("usuarios").child(userPhone).child("llamadas").child(callId).removeValue()
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userPhone)
                    .collection("llamadas")
                    .document(callId)
                    .delete()
            } catch (_: Exception) {}
        }
}

private fun vaciarHistorialDeBD(userPhone: String) {
    if (userPhone.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    rtdb.getReference("usuarios").child(userPhone).child("llamadas").removeValue()
}

private fun guardarEnAgendaDelDispositivo(context: Context, nombre: String, telefono: String) {
    try {
        val intent = android.content.Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, nombre)
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, telefono)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("ContactoAgenda", "Error al abrir la agenda de contactos", e)
    }
}

private fun guardarContactoEnBD(userPhone: String, nombre: String, telefono: String, detalle: String) {
    if (userPhone.isBlank()) return

    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val contactsRef = rtdb.getReference("usuarios").child(userPhone).child("contactos")
    val contactId = contactsRef.push().key ?: System.currentTimeMillis().toString()

    val contactMap = hashMapOf(
        "id" to contactId,
        "nombre" to nombre,
        "telefono" to telefono,
        "detalle" to detalle,
        "fechaAgregado" to dateStr
    )

    contactsRef.child(contactId).setValue(contactMap)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userPhone)
                    .collection("contactos")
                    .document(contactId)
                    .set(contactMap)
            } catch (_: Exception) {}
        }
}

private fun actualizarContactoEnBD(userPhone: String, contactId: String, nombre: String, telefono: String, detalle: String) {
    if (userPhone.isBlank() || contactId.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    val updates = hashMapOf<String, Any>(
        "nombre" to nombre,
        "telefono" to telefono,
        "detalle" to detalle
    )

    rtdb.getReference("usuarios").child(userPhone).child("contactos").child(contactId).updateChildren(updates)
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userPhone)
                    .collection("contactos")
                    .document(contactId)
                    .update(updates)
            } catch (_: Exception) {}
        }
}

private fun eliminarContactoDeBD(userPhone: String, contactId: String) {
    if (userPhone.isBlank() || contactId.isBlank()) return

    val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
    rtdb.getReference("usuarios").child(userPhone).child("contactos").child(contactId).removeValue()
        .addOnSuccessListener {
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userPhone)
                    .collection("contactos")
                    .document(contactId)
                    .delete()
            } catch (_: Exception) {}
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LlamadasContent(userPhone: String) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    val cleanPhone = userPhone.trim()

    val contactos = remember { mutableStateListOf<Contacto>() }
    val historialLlamadas = remember { mutableStateListOf<LlamadaRegistro>() }
    var isLoadingHistorial by remember { mutableStateOf(true) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    DisposableEffect(cleanPhone) {
        if (cleanPhone.isBlank()) return@DisposableEffect onDispose {}

        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
        val contactsRef = rtdb.getReference("usuarios").child(cleanPhone).child("contactos")

        val contactsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Contacto>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    val telefono = child.child("telefono").getValue(String::class.java) ?: ""
                    if (id.isNotBlank() && telefono.isNotBlank()) {
                        list.add(Contacto(id = id, nombre = nombre, telefono = telefono))
                    }
                }
                contactos.clear()
                contactos.addAll(list.sortedBy { it.nombre.lowercase() })
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        contactsRef.addValueEventListener(contactsListener)

        onDispose {
            contactsRef.removeEventListener(contactsListener)
        }
    }

    DisposableEffect(cleanPhone) {
        if (cleanPhone.isBlank()) {
            isLoadingHistorial = false
            return@DisposableEffect onDispose {}
        }

        val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
        val callsRef = rtdb.getReference("usuarios").child(cleanPhone).child("llamadas")

        val callsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LlamadaRegistro>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val nombreContacto = child.child("nombreContacto").getValue(String::class.java) ?: "Desconocido"
                    val telefono = child.child("telefono").getValue(String::class.java) ?: ""
                    val fechaHora = child.child("fechaHora").getValue(String::class.java) ?: ""
                    val tipo = child.child("tipo").getValue(String::class.java) ?: "saliente"

                    if (id.isNotBlank() && telefono.isNotBlank()) {
                        list.add(
                            LlamadaRegistro(
                                id = id,
                                nombreContacto = nombreContacto,
                                telefono = telefono,
                                fechaHora = fechaHora,
                                tipo = tipo
                            )
                        )
                    }
                }
                historialLlamadas.clear()
                historialLlamadas.addAll(list.reversed())
                isLoadingHistorial = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingHistorial = false
            }
        }

        callsRef.addValueEventListener(callsListener)

        onDispose {
            callsRef.removeEventListener(callsListener)
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardBg,
            title = {
                Text("¿Vaciar historial?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Se eliminarán todas las llamadas registradas.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        vaciarHistorialDeBD(cleanPhone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Vaciar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearHistoryDialog = false },
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = CardBg,
            contentColor = AppLightBlue,
            indicator = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Dialpad,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Marcador", fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = AppLightBlue,
                unselectedContentColor = TextSecondary
            )

            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Historial (${historialLlamadas.size})", fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = AppLightBlue,
                unselectedContentColor = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedSubTab == 0) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (contactos.isNotEmpty()) {
                        Text(
                            text = "Marcado Rápido:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(contactos, key = { it.id }) { contacto ->
                                Surface(
                                    onClick = { phoneNumber = contacto.telefono },
                                    shape = RoundedCornerShape(20.dp),
                                    color = CardBg,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(AppBlue.copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (contacto.nombre.isNotBlank()) contacto.nombre.take(1).uppercase() else "C",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = AppLightBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = contacto.nombre,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (phoneNumber.isBlank()) "Ingresa número..." else phoneNumber,
                            fontSize = if (phoneNumber.length > 12) 22.sp else 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phoneNumber.isBlank()) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (phoneNumber.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (phoneNumber.isNotEmpty()) {
                                        phoneNumber = phoneNumber.dropLast(1)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Borrar",
                                    tint = AppLightBlue
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = BorderDark,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    dialPadMatrix.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { keyInfo ->
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                phoneNumber += keyInfo.number
                                            },
                                            onLongClick = {
                                                if (keyInfo.number == "0") {
                                                    phoneNumber += "+"
                                                }
                                            }
                                        ),
                                    shape = CircleShape,
                                    color = CardBg,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = keyInfo.number,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (keyInfo.letters.isNotBlank()) {
                                            Text(
                                                text = keyInfo.letters,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val cleanTel = phoneNumber.trim()
                            if (cleanTel.isNotBlank()) {
                                val matchContact = contactos.find { it.telefono.trim() == cleanTel }
                                val contactName = matchContact?.nombre ?: "Desconocido"
                                realizarYLlamadaYRegistrar(context, cleanPhone, cleanTel, contactName)
                            }
                        },
                        enabled = phoneNumber.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            disabledContainerColor = Color(0xFF162332)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Llamar",
                            tint = if (phoneNumber.isNotBlank()) Color.White else TextSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (historialLlamadas.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Llamadas recientes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        TextButton(onClick = { showClearHistoryDialog = true }) {
                            Text("Vaciar historial", color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }

                if (isLoadingHistorial) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppLightBlue)
                    }
                } else if (historialLlamadas.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = AppLightBlue,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Historial vacío",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Las llamadas que realices desde el marcador o tus contactos se registrarán aquí.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(historialLlamadas, key = { it.id }) { registro ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFF1B324B), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.CallMade,
                                                contentDescription = "Llamada saliente",
                                                tint = Color(0xFF22C55E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = if (registro.nombreContacto.isNotBlank() && registro.nombreContacto != "Desconocido") {
                                                    registro.nombreContacto
                                                } else {
                                                    registro.telefono
                                                },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (registro.nombreContacto.isNotBlank() && registro.nombreContacto != "Desconocido") {
                                                Text(
                                                    text = registro.telefono,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "Saliente • ${registro.fechaHora}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                realizarYLlamadaYRegistrar(
                                                    context,
                                                    cleanPhone,
                                                    registro.telefono,
                                                    registro.nombreContacto
                                                )
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Phone,
                                                contentDescription = "Llamar",
                                                tint = Color(0xFF22C55E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                eliminarLlamadaDeBD(cleanPhone, registro.id)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Eliminar de historial",
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
