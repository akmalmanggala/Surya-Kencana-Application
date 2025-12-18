package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityLoginBinding
import com.example.suryakencanaapp.model.LoginRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek sesi login
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        if (sharedPref.contains("TOKEN")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(username, password)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = AlertDialog.Builder(this)
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false)
                loadingDialog = builder.create()
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
            binding.etUsername.isEnabled = false
            binding.etPassword.isEnabled = false
            binding.btnLogin.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.etUsername.isEnabled = true
            binding.etPassword.isEnabled = true
            binding.btnLogin.isEnabled = true
        }
    }

    private fun performLogin(user: String, pass: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)

                val request = LoginRequest(user, pass)
                val response = ApiClient.instance.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // 1. Ambil Token
                    val token = loginResponse.token

                    // 2. Ambil Role (PERBAIKAN UTAMA DI SINI)
                    // Mengakses 'adminData' sesuai nama di LoginResponse.kt
                    val role = loginResponse.adminData.role

                    // 3. Simpan ke SharedPreferences
                    val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("TOKEN", token)
                        putString("ROLE", role)
                        apply()
                    }

                    Toast.makeText(this@LoginActivity, "Login Berhasil", Toast.LENGTH_SHORT).show()

                    // Pindah ke MainActivity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login Gagal: Cek Username/Password", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Ignore
                } else {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                setLoading(false)
            }
        }
    }
}