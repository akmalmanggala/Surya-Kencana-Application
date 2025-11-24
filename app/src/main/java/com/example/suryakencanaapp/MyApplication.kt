package com.example.suryakencanaapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

// Class ini turunan dari Application, bukan AppCompatActivity
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // INI KODE SAKTINYA
        // Mematikan Dark Mode secara global untuk satu aplikasi penuh
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}