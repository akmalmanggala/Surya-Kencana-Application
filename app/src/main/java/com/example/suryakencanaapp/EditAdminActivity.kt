package com.example.suryakencanaapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddAdminBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAdminBinding
    private var adminId: Int = 0
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ubah Judul & Tombol
        binding.tvPageTitle.text = "Edit Admin"
        binding.btnSave.text = "Simpan Perubahan"
        binding.password.hint = "Password Baru"

        // 2. Ubah Info Password sesuai permintaan Anda
        binding.cardPasswordInfo.visibility = View.VISIBLE

        // 3. Ambil Data
        adminId = intent.getIntExtra("ID", 0)
        val username = intent.getStringExtra("USERNAME")

        // 4. Isi Form
        binding.etUsername.setText(username)

        // Listener
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            updateAdmin()
        }
    }

    // --- HELPER LOADING ---
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
            binding.btnSave.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSave.isEnabled = true
        }
    }

    private fun updateAdmin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val reqUsername = username.toRequestBody("text/plain".toMediaTypeOrNull())

                val reqPasswordPart = if (password.isNotEmpty()) {
                    if (password.length < 6) {
                        Toast.makeText(this@EditAdminActivity, "Password baru min 6 karakter", Toast.LENGTH_SHORT).show()
                        setLoading(false) // Matikan loading jika validasi gagal
                        return@launch
                    }
                    MultipartBody.Part.createFormData("password", password)
                } else {
                    null
                }

                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiClient.instance.updateAdmin(
                    token = "Bearer $token",
                    id = adminId,
                    username = reqUsername,
                    password = reqPasswordPart,
                    method = reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EditAdminActivity, "Admin Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UPDATE_ADMIN", "Gagal: $errorBody")
                    Toast.makeText(this@EditAdminActivity, "Gagal update: Cek Logcat", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                // 2. Sembunyikan Overlay
                setLoading(false)
            }
        }
    }
}