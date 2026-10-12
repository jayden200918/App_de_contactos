package com.example.app_de_contactos

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val app = FirebaseApp.getInstance()
        Log.d("FirebaseCheck", "¡Firebase conectado correctamente!: ${app.name}")

        // Guardar contacto de prueba en Realtime Database
        probarConexionRealtimeDatabase()
        // Guardar contacto de prueba en Firestore
        probarConexionFirestore()
    }

    private fun probarConexionRealtimeDatabase() {
        val databaseUrl = "https://app-de-contacto-default-rtdb.firebaseio.com"
        val database = FirebaseDatabase.getInstance(databaseUrl)
        val myRef = database.getReference("contactos_prueba")

        val contactoPrueba = hashMapOf(
            "nombre" to "Contacto de Prueba RTDB",
            "telefono" to "987654321",
            "email" to "prueba_rtdb@example.com"
        )

        myRef.push().setValue(contactoPrueba)
            .addOnSuccessListener {
                Log.d("FirebaseCheck", "¡Dato guardado con ÉXITO en Realtime Database!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseCheck", "Error al guardar en Realtime Database", e)
            }
    }

    private fun probarConexionFirestore() {
        val db = FirebaseFirestore.getInstance()
        val contactoPrueba = hashMapOf(
            "nombre" to "Contacto de Prueba Firestore",
            "telefono" to "123456789",
            "email" to "prueba@example.com"
        )

        db.collection("contactos")
            .add(contactoPrueba)
            .addOnSuccessListener { documentReference ->
                Log.d("FirebaseCheck", "¡Contacto guardado con ÉXITO en Firestore! ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseCheck", "Error al guardar contacto en Firestore", e)
            }
    }
}
