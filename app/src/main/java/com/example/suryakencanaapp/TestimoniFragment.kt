package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.model.Testimoni
import com.example.suryakencanaapp.api.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestimoniFragment : Fragment(R.layout.fragment_testimoni) {

    private lateinit var rvTestimoni: RecyclerView
    private lateinit var testimoniAdapter: TestimoniAdapter
    private lateinit var etSearch: EditText // 1. Deklarasi EditText

    private var searchJob: Job? = null // 2. Variable untuk delay pencarian

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        rvTestimoni = view.findViewById(R.id.rvTestimoni)
        rvTestimoni.layoutManager = LinearLayoutManager(context)

        val btnAdd = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddTestimoni)
        btnAdd.setOnClickListener {
            // Pindah ke halaman AddTestimoniActivity
            val intent = Intent(requireContext(), AddTestimoniActivity::class.java)
            startActivity(intent)
        }

        // Inisialisasi Adapter dengan list kosong dulu agar tidak error
        testimoniAdapter = TestimoniAdapter(listOf()) { dataYangMauDihapus ->
            showDeleteConfirmation(dataYangMauDihapus)
        }
        rvTestimoni.adapter = testimoniAdapter

        // Setup Search
        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        fetchTestimonies()
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Batalkan pencarian sebelumnya jika user masih mengetik
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    // Tunggu 800ms (biar user selesai ngetik dulu)
                    delay(800)

                    val keyword = s.toString().trim()
                    if (keyword.isNotEmpty()) {
                        fetchTestimonies(keyword) // Cari dengan kata kunci
                    } else {
                        fetchTestimonies(null) // Ambil semua data
                    }
                }
            }
        })
    }

    // Fungsi fetch fleksibel (bisa pakai keyword atau null)
    private fun fetchTestimonies(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                // Panggil API (ApiClient akan otomatis pasang ?search=keyword)
                val response = ApiClient.instance.getTestimonies(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    val sortedList = listData.sortedByDescending { it.id }

                    if (sortedList.isNotEmpty()) {
                        testimoniAdapter.updateData(sortedList)
                    } else {
                        testimoniAdapter.updateData(emptyList()) // Kosongkan jika tidak ada data

                        // Opsional: Toast info jika bukan sedang search
                        if (keyword == null) {
                            Toast.makeText(context, "Belum ada testimoni", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}")
                // Jangan tampilkan toast error saat user sedang ngetik, nanti mengganggu
            }
        }
    }

    private fun showDeleteConfirmation(data: Testimoni) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Testimoni")
            .setMessage("Yakin ingin menghapus testimoni dari ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
                // Jika user pilih YA, panggil API
                if (data.id != null) {
                    deleteTestimoniApi(data.id)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // 3. PANGGIL API DELETE
    private fun deleteTestimoniApi(id: Int) {
        // Ambil Token
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Sesi habis", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteTestimoni("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    // Refresh data agar yang dihapus hilang dari layar
                    fetchTestimonies()
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}