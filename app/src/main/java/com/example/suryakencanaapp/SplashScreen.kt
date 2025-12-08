package com.example.suryakencanaapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // Mengatur padding untuk sistem bars (opsional, tapi bagus untuk edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- BAGIAN TIMER (VERSI KOTLIN) ---
        // 1. Kita gunakan Looper.getMainLooper() agar Handler berjalan di thread utama
        // 2. Kita gunakan 'postDelayed' dengan lambda { ... } ala Kotlin
        Handler(Looper.getMainLooper()).postDelayed({
            // Pindah ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Tutup Splash Screen agar tidak bisa di-back
        }, 3000) // Waktu delay 3000ms (3 detik)
    }
}