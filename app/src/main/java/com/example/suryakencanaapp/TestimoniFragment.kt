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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.suryakencanaapp.adapter.TestimoniAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Testimoni
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestimoniFragment : Fragment(R.layout.fragment_testimoni) {

    private lateinit var rvTestimoni: RecyclerView
    private lateinit var testimoniAdapter: TestimoniAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init SwipeRefresh
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // 2. Setup RecyclerView
        rvTestimoni = view.findViewById(R.id.rvTestimoni)
        rvTestimoni.layoutManager = LinearLayoutManager(context)

        // Init Adapter (Pastikan TestimoniAdapter sudah Anda buat dengan callback delete/edit)
        testimoniAdapter = TestimoniAdapter(listOf()) { testimoniToDelete ->
            showDeleteConfirmation(testimoniToDelete)
        }
        rvTestimoni.adapter = testimoniAdapter

        // 3. Listener Manual Refresh
        swipeRefresh.setOnRefreshListener {
            val keyword = etSearch.text.toString().trim()
            fetchTestimonies(if (keyword.isNotEmpty()) keyword else null)
        }

        // 4. Setup Tombol Tambah
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddTestimoni)
        btnAdd.setOnClickListener {
            // Arahkan ke Activity Tambah Testimoni (Ganti jika namanya beda)
            startActivity(Intent(requireContext(), AddTestimoniActivity::class.java))
        }

        // 5. Setup Pencarian
        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    // 6. Load Otomatis saat masuk halaman
    override fun onResume() {
        super.onResume()

        // 1. Cek Keamanan: Pastikan Adapter DAN EditText sudah siap
        if (::testimoniAdapter.isInitialized && ::etSearch.isInitialized) {

            // 2. Ambil kata kunci terakhir (agar pencarian tidak hilang)
            val keyword = etSearch.text.toString().trim()

            // 3. Panggil fetch dengan kata kunci tersebut
            fetchTestimonies(if (keyword.isNotEmpty()) keyword else null)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(800)
                    val keyword = s.toString().trim()
                    if (keyword.isNotEmpty()) {
                        fetchTestimonies(keyword)
                    } else {
                        fetchTestimonies(null)
                    }
                }
            }
        })
    }

    private fun fetchTestimonies(keyword: String? = null) {
        // Tampilkan loading
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getTestimonies(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!
                    // Urutkan terbaru di atas
                    val sortedList = listData.sortedByDescending { it.id }
                    testimoniAdapter.updateData(sortedList)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TESTIMONI_API", "Error: ${e.message}")
            } finally {
                // Matikan loading
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showDeleteConfirmation(data: Testimoni) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Testimoni")
            .setMessage("Hapus ulasan dari ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
                // SOLUSI: Cek dulu apakah ID-nya ada (tidak null)
                data.id?.let { idPasti ->
                    deleteTestimoniApi(idPasti)
                } ?: run {
                    Toast.makeText(context, "Gagal: ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteTestimoniApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteTestimoni("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    fetchTestimonies() // Refresh otomatis
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}