package com.example.app_de_contactos

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

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
                if (!isLoggedIn) {
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
                    text = if (isRegisterMode) "Crea tu cuenta en Firebase" else "Bienvenido de nuevo",
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
                    isError = errorMessage != null && username.isBlank(),
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
                    isError = errorMessage != null && phone.isBlank(),
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
                            // MODO REGISTRO: Guardar en Realtime Database (y secundario Firestore)
                            rtdb.getReference("usuarios").child(cleanPhone).setValue(userMap)
                                .addOnSuccessListener {
                                    try {
                                        db.collection("usuarios").document(cleanPhone).set(userMap)
                                    } catch (_: Exception) {}

                                    isLoading = false
                                    isRegisterMode = false
                                    successMessage = "¡Registro exitoso! Ya puedes iniciar sesión."
                                }
                                .addOnFailureListener { e ->
                                    // Si falla RTDB, intentar en Firestore
                                    db.collection("usuarios").document(cleanPhone).set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            isRegisterMode = false
                                            successMessage = "¡Registro exitoso! Ya puedes iniciar sesión."
                                        }
                                        .addOnFailureListener { e2 ->
                                            isLoading = false
                                            errorMessage = "Error al registrar: ${e2.localizedMessage ?: e.localizedMessage}"
                                        }
                                }
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
                            text = if (isRegisterMode) "REGISTRARSE EN FIREBASE" else "INICIAR SESIÓN",
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
                                contentDescription = "Foto de perfil",
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
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                                HorizontalDivider(color = BorderDark)
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
                                    onLogout()
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
                // Tab 1: Chats
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chats"
                        )
                    },
                    label = {
                        Text(
                            text = "Chats",
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
                0 -> ChatsContent()
                1 -> NotasContent()
                2 -> LlamadasContent()
            }
        }
    }
}

@Composable
fun ChatsContent() {
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
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Lista de Chats",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aquí aparecerán tus conversaciones recientes.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NotasContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = WhatsAppGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pestaña de Notas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aquí puedes guardar y consultar tus notas personales.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
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
