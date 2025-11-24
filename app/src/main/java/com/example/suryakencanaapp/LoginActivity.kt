package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.model.LoginRequest // Pastikan import ini benar
import com.example.suryakencanaapp.api.ApiClient     // Pastikan import ini benar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CEK SESSION (Auto Login)
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", null)

        if (token != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // 2. INISIALISASI VIEW (Sesuai ID di XML Baru)
        // Gunakan TextInputEditText karena kita pakai TextInputLayout di XML
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar) // Tambahan baru

        // 3. LOGIC TOMBOL LOGIN
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {

                // TAMPILKAN LOADING & MATIKAN TOMBOL (Agar tidak diklik 2x)
                progressBar.visibility = View.VISIBLE
                btnLogin.isEnabled = false
                btnLogin.text = "Loading..."

                // Panggil fungsi login
                performLogin(username, password, btnLogin, progressBar)

            } else {
                Toast.makeText(this, "Username dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Saya tambahkan parameter button & progressbar agar bisa diubah statusnya
    private fun performLogin(
        username: String,
        pass: String,
        btnLogin: MaterialButton,
        progressBar: ProgressBar
    ) {
        lifecycleScope.launch {
            try {
                val request = LoginRequest(username, pass)
                val response = ApiClient.instance.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Toast.makeText(this@LoginActivity, "Halo, ${body.adminData.username}", Toast.LENGTH_LONG).show()

                        // Simpan Session
                        // Pastikan di Model JSON response Anda ada field 'adminData'
                        // Jika di JSON namanya 'admin', ganti 'body.adminData' jadi 'body.admin'
                        saveSession(body.token, body.role, body.adminData.username)

                        // Pindah Dashboard
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login Gagal. Cek Username/Password", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error Koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                // BAGIAN PENTING:
                // Apapun yang terjadi (Sukses/Gagal/Error), kembalikan tombol seperti semula
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnLogin.text = "LOGIN"
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