package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.LoginRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Langsung Cek Session (Tanpa Delay)
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", null)

        if (token != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 2. Jika belum login, tampilkan layar
        setContentView(R.layout.activity_login)

        // Init View & Listener
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                btnLogin.text = "Loading..."
                btnLogin.isClickable = false

                performLogin(username, password, btnLogin, progressBar)
            } else {
                Toast.makeText(this, "Wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(username: String, pass: String, btn: MaterialButton, progress: ProgressBar) {
        lifecycleScope.launch {
            try {
                val request = LoginRequest(username, pass)
                val response = ApiClient.instance.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Toast.makeText(this@LoginActivity, "Halo, ${body.adminData.username}", Toast.LENGTH_LONG).show()

                    saveSession(body.token, body.role, body.adminData.username)

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login Gagal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
                btn.text = "LOGIN"
                btn.isClickable = true
            }
        }
    }

    private fun saveSession(token: String, role: String?, username: String) {
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("TOKEN", token)
            putString("ROLE", role)
            putString("USERNAME", username)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }
    }
}