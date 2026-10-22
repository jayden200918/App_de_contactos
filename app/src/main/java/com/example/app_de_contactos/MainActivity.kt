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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
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

// Colores del tema oscuro de la app
val DarkBg = Color(0xFF0B141A)
val CardBg = Color(0xFF111B21)
val TextPrimary = Color(0xFFE9EDEF)
val TextSecondary = Color(0xFF8696A0)
val WhatsAppGreen = Color(0xFF00A884)
val ChipSelectedBg = Color(0xFF0C3A2D)
val BottomBarBg = Color(0xFF0B141A)
val BorderDark = Color(0xFF2A3942)
val ErrorRed = Color(0xFFFF5555)

// Categorías de notas disponibles
data class CategoriaNota(
    val nombre: String,
    val color: Color,
    val hex: String
)

val categoriasDisponibles = listOf(
    CategoriaNota("Personal", WhatsAppGreen, "#00A884"),
    CategoriaNota("Trabajo", Color(0xFF3498DB), "#3498DB"),
    CategoriaNota("Importante", Color(0xFFE67E22), "#E67E22"),
    CategoriaNota("Idea", Color(0xFF9B59B6), "#9B59B6")
)

fun parseHexColor(hex: String, fallback: Color = WhatsAppGreen): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

// Modelo de datos para las notas
data class Nota(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val categoria: String = "Personal",
    val colorHex: String = "#00A884"
)

// Modelo de datos para los contactos (con campo de detalle/nota)
data class Contacto(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val detalle: String = "",
    val fechaAgregado: String = ""
)

// Gestor de sesión persistente usando SharedPreferences
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

        // Pruebas de conexión Firebase
        probarConexionesFirebase()

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

    private fun probarConexionesFirebase() {
        try {
            val app = FirebaseApp.getInstance()
            Log.d("FirebaseCheck", "¡Firebase conectado correctamente!: ${app.name}")
        } catch (e: Exception) {
            Log.e("FirebaseCheck", "Error Firebase", e)
        }
    }
}

@Composable
fun ContactosAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = WhatsAppGreen,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun LoginScreen(onLoginSuccess: (phone: String, name: String) -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) } // false: Iniciar Sesión, true: Registrarse
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
        // Franja superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .background(Color(0xFF0F1A1C)),
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
                    color = WhatsAppGreen
                )
            }
        }

        // Tarjeta flotante central
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

                // Campo 1: Nombre de usuario
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
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    isError = (errorMessage != null && username.isBlank()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = BorderDark,
                        errorBorderColor = ErrorRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo 2: Número de teléfono
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = "Número de teléfono",
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    isError = (errorMessage != null && phone.isBlank()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = BorderDark,
                        errorBorderColor = ErrorRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mensajes de respuesta (Error o Éxito)
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
                        color = WhatsAppGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Principal (Iniciar Sesión o Registrarse)
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
                            // MODO REGISTRO: Guardar en Realtime Database y Firestore, e ingresar de inmediato
                            rtdb.getReference("usuarios").child(cleanPhone).setValue(userMap)
                            try {
                                db.collection("usuarios").document(cleanPhone).set(userMap)
                            } catch (_: Exception) {}

                            isLoading = false
                            onLoginSuccess(cleanPhone, cleanUser)
                        } else {
                            // MODO INICIO DE SESIÓN: Buscar primero en Realtime Database
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
                                        // Buscar en Firestore por si acaso estuvo registrado allí
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
                                    // Si falla RTDB, intentar buscar en Firestore
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
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "REGISTRARSE" else "INICIAR SESIÓN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cambiar entre Iniciar Sesión y Registrarse
                Text(
                    text = if (isRegisterMode) "¿YA TIENES CUENTA?\nINICIA SESIÓN" else "¿NO TIENES CUENTA?\nREGÍSTRATE",
                    color = WhatsAppGreen,
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

    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Notas, 2: Llamadas
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Diálogo profesional de confirmación de cierre de sesión
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
                                contentDescription = "Perfil de usuario",
                                tint = TextPrimary,
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
                                                    .background(WhatsAppGreen, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color.Black
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
                // Tab 1: Contactos
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
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = ChipSelectedBg,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                // Tab 2: Notas
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
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = ChipSelectedBg,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                // Tab 3: Llamadas
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
                        selectedIconColor = TextPrimary,
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
                2 -> LlamadasContent()
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
                        color = Color(0xFF16232B),
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
                                    .background(WhatsAppGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
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
                    text = "¿Estás seguro de que deseas salir de tu cuenta? Tendrás que volver a iniciar sesión para acceder a tus conversaciones.",
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

    val cleanPhone = userPhone.trim()
    val context = LocalContext.current

    // Cargar contactos desde Firebase Realtime Database
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
                contactos.addAll(list.sortedBy { it.nombre.lowercase() }) // Ordenar alfabéticamente
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

    // Diálogo para agregar nuevo contacto
    if (showAddDialog) {
        AgregarContactoDialog(
            onDismiss = { showAddDialog = false },
            onSaveInApp = { nombre, telefono, detalle ->
                showAddDialog = false
                guardarContactoEnFirebase(cleanPhone, nombre, telefono, detalle)
            },
            onSaveToDevice = { nombre, telefono, detalle ->
                showAddDialog = false
                guardarContactoEnFirebase(cleanPhone, nombre, telefono, detalle)
                guardarEnAgendaDelDispositivo(context, nombre, telefono)
            }
        )
    }

    // Diálogo para consultar detalle del contacto
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
                selectedContactoForDetail = null
                if (tel.isNotBlank()) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_DIAL,
                            android.net.Uri.parse("tel:$tel")
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        )
    }

    // Diálogo para editar contacto
    if (contactoToEdit != null) {
        EditarContactoDialog(
            contacto = contactoToEdit!!,
            onDismiss = { contactoToEdit = null },
            onSave = { nuevoNombre, nuevoTelefono, nuevoDetalle ->
                val targetId = contactoToEdit?.id ?: ""
                contactoToEdit = null
                if (targetId.isNotBlank()) {
                    actualizarContactoEnFirebase(cleanPhone, targetId, nuevoNombre, nuevoTelefono, nuevoDetalle)
                }
            }
        )
    }

    // Diálogo para confirmar eliminación de contacto
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
                            eliminarContactoDeFirebase(cleanPhone, target.id)
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
            // Campo de Búsqueda
            if (contactos.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar contacto o detalle...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
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
                    CircularProgressIndicator(color = WhatsAppGreen)
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
                        tint = WhatsAppGreen,
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
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:${contacto.telefono}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }

        // Botón Flotante (FAB) para Agregar Nuevo Contacto
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = WhatsAppGreen,
            contentColor = Color.Black,
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
    onCallClick: () -> Unit
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
                // Avatar circular con inicial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(WhatsAppGreen.copy(alpha = 0.2f), CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (contacto.nombre.isNotBlank()) contacto.nombre.take(1).uppercase() else "C",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = WhatsAppGreen
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

                    // Muestra el detalle del contacto si existe
                    if (contacto.detalle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = contacto.detalle,
                                fontSize = 12.sp,
                                color = WhatsAppGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón para llamar
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = "Llamar a ${contacto.nombre}",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botón para editar
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar contacto",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botón para eliminar
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar contacto",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
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
    onCall: () -> Unit
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
                            .background(WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (contacto.nombre.isNotBlank()) contacto.nombre.take(1).uppercase() else "C",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
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
                    color = Color(0xFF16232B),
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
                                tint = WhatsAppGreen,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                    Text("Eliminar", color = ErrorRed)
                }

                Row {
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
                        Text("Editar", color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Llamar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
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
                            .background(WhatsAppGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = WhatsAppGreen,
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
                // Campo 1: Nombre del Contacto
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
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo 2: Número de Teléfono
                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        telefono = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo 3: Detalle o Nota del contacto
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
                            tint = TextSecondary
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
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
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Guardar en la App", color = Color.Black, fontWeight = FontWeight.Bold)
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
                    border = BorderStroke(1.dp, WhatsAppGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
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
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        telefono = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Número de teléfono", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
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
                        focusedBorderColor = WhatsAppGreen,
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
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
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

    // Escuchar notas en tiempo real desde Firebase Realtime Database
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
                    val colorHex = child.child("colorHex").getValue(String::class.java) ?: "#00A884"

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
                notas.addAll(list.reversed()) // Mostrar las más recientes primero
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

    // Formulario mejorado para agregar nota
    if (showAddDialog) {
        AgregarNotaDialog(
            onDismiss = { showAddDialog = false },
            onSave = { titulo, descripcion, categoria, colorHex ->
                showAddDialog = false
                guardarNotaEnFirebase(cleanPhone, titulo, descripcion, categoria, colorHex)
            }
        )
    }

    // Diálogo para confirmar eliminación de nota
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
                            eliminarNotaDeFirebase(cleanPhone, target.id)
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

    // Diálogo para consultar/ver la nota al tocarla
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
                CircularProgressIndicator(color = WhatsAppGreen)
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
                    tint = WhatsAppGreen,
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

        // Botón Flotante (FAB) para Agregar Nota
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = WhatsAppGreen,
            contentColor = Color.Black,
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
            // Franja de color indicador de categoría
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
                // Fila superior: Chip de categoría + Botones de editar y eliminar
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

                // Título de la nota
                Text(
                    text = nota.titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Descripción de la nota
                if (nota.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nota.descripcion,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }

                // Fecha
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
                    // MODO LECTURA / CONSULTA
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
                                tint = WhatsAppGreen,
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
                        color = Color(0xFF16232B),
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
                    // MODO EDICIÓN
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
                                color = if (isSelected) cat.color.copy(alpha = 0.25f) else Color(0xFF16232B),
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
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        actualizarNotaEnFirebase(
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
                            color = Color.Black,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
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
                            .background(WhatsAppGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = WhatsAppGreen,
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
                // Selector de Categoría
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
                            color = if (isSelected) cat.color.copy(alpha = 0.25f) else Color(0xFF16232B),
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

                // Campo 1: Título de la Nota
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

                // Campo 2: Descripción de la Nota
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
                Text("Guardar Nota", color = Color.Black, fontWeight = FontWeight.Bold)
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

// Funciones auxiliares para sincronizar notas con Firebase
private fun guardarNotaEnFirebase(
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

    // Guardar en Realtime Database
    notesRef.child(noteId).setValue(noteMap)
        .addOnSuccessListener {
            // Guardar también en Firestore como respaldo
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

private fun actualizarNotaEnFirebase(
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

private fun eliminarNotaDeFirebase(phone: String, noteId: String) {
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

// Funciones auxiliares de contactos para Firebase y agenda
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

private fun guardarContactoEnFirebase(userPhone: String, nombre: String, telefono: String, detalle: String) {
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

private fun actualizarContactoEnFirebase(userPhone: String, contactId: String, nombre: String, telefono: String, detalle: String) {
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

private fun eliminarContactoDeFirebase(userPhone: String, contactId: String) {
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

@Composable
fun LlamadasContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Phone,
            contentDescription = null,
            tint = WhatsAppGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Historial de Llamadas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aquí verás el registro de llamadas recientes.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
