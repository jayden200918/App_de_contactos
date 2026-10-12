package com.example.app_de_contactos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

// Colores de tema oscuro
val DarkBg = Color(0xFF0B141A)
val CardBg = Color(0xFF111B21)
val TextPrimary = Color(0xFFE9EDEF)
val TextSecondary = Color(0xFF8696A0)
val WhatsAppGreen = Color(0xFF00A884)
val ChipSelectedBg = Color(0xFF0C3A2D)
val BottomBarBg = Color(0xFF0B141A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pruebas de conexión Firebase
        probarConexionesFirebase()

        setContent {
            ContactosAppTheme {
                MainScreen()
            }
        }
    }

    private fun probarConexionesFirebase() {
        try {
            val app = FirebaseApp.getInstance()
            Log.d("FirebaseCheck", "¡Firebase conectado correctamente!: ${app.name}")

            // Firebase Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("contactos")
                .add(hashMapOf("nombre" to "Contacto Compose", "telefono" to "123456789"))
                .addOnSuccessListener { Log.d("FirebaseCheck", "Firestore OK: ${it.id}") }

            // Firebase Realtime Database
            val rtdb = FirebaseDatabase.getInstance("https://app-de-contacto-default-rtdb.firebaseio.com")
            rtdb.getReference("contactos_prueba").push()
                .setValue(hashMapOf("nombre" to "Contacto Compose RTDB"))
                .addOnSuccessListener { Log.d("FirebaseCheck", "RTDB OK") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Notas, 2: Llamadas

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
