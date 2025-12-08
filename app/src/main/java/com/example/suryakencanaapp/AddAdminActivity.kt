package com.example.suryakencanaapp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddAdminActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_admin)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createAdmin(username, password)
        }
    }

    private fun createAdmin(user: String, pass: String) {
        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Uploading..."

                val response = ApiClient.instance.addAdmin("Bearer $token", user, pass)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddAdminActivity, "Admin Berhasil Ditambahkan!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@AddAdminActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Tambah Admin"
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Tambah Admin"
            }
        }
    }
}