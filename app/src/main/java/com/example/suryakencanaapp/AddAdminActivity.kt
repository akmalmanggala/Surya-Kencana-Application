package com.example.suryakencanaapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddAdminBinding
import kotlinx.coroutines.launch

class AddAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

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
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Uploading..."

                val response = ApiClient.instance.addAdmin("Bearer $token", user, pass)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddAdminActivity, "Admin Berhasil Ditambahkan!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@AddAdminActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Tambah Admin"
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Tambah Admin"
            }
        }
    }
}