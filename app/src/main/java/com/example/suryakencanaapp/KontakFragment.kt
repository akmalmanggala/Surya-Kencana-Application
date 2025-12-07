package com.example.suryakencanaapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class KontakFragment : Fragment(R.layout.fragment_kontak) { // Pastikan nama XML benar

    // UI Variables
    private lateinit var etEmail: TextInputEditText
    private lateinit var etWhatsapp: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etMaps: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi View
        etEmail = view.findViewById(R.id.etEmail)
        etWhatsapp = view.findViewById(R.id.etWhatsapp)
        etAddress = view.findViewById(R.id.etAddress)
        etMaps = view.findViewById(R.id.etMaps)
        btnSave = view.findViewById(R.id.btnSaveContact)

        // 2. Button Save Listener
        btnSave.setOnClickListener {
            saveContactData()
        }

        // 3. Ambil Data Awal
        fetchContactData()
    }

    private fun fetchContactData() {
        lifecycleScope.launch {
            try {
                // Panggil API
                val response = ApiClient.instance.getContact()

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    if (listData.isNotEmpty()) {
                        // Ambil data pertama (index 0)
                        val data = listData[0]

                        // Isi Form
                        etEmail.setText(data.email)
                        etWhatsapp.setText(data.phone)
                        etAddress.setText(data.address)
                        etMaps.setText(data.mapUrl)
                    }
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CONTACT_API", "Error: ${e.message}")
                Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveContactData() {
        val email = etEmail.text.toString().trim()
        val phone = etWhatsapp.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val maps = etMaps.text.toString().trim()

        // Validasi sederhana
        if (email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(context, "Email, WA, dan Alamat wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil Token
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Menyimpan..."

                // Kirim ke API
                val response = ApiClient.instance.updateContact(
                    "Bearer $token",
                    email,
                    phone,
                    address,
                    maps
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, "Kontak Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    // Cek error message dari server jika ada
                    val errorMsg = response.errorBody()?.string()
                    Log.e("CONTACT_UPDATE", "Error: $errorMsg")
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
            }
        }
    }
}