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
import android.widget.TextView
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
import kotlinx.coroutines.launch

class TestimoniFragment : Fragment(R.layout.fragment_testimoni) {

    private lateinit var rvTestimoni: RecyclerView
    private lateinit var testimoniAdapter: TestimoniAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView

    // 1. Variabel untuk menyimpan semua data (Master Data)
    private var allTestimoniList: List<Testimoni> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        rvTestimoni = view.findViewById(R.id.rvTestimoni)
        etSearch = view.findViewById(R.id.etSearch)

        rvTestimoni.layoutManager = LinearLayoutManager(context)

        testimoniAdapter = TestimoniAdapter(listOf()) { testimoniToDelete ->
            showDeleteConfirmation(testimoniToDelete)
        }
        rvTestimoni.adapter = testimoniAdapter

        // Listener Refresh
        swipeRefresh.setOnRefreshListener {
            fetchTestimonies()
        }

        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddTestimoni)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddTestimoniActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        // Ambil data terbaru setiap halaman dibuka
        fetchTestimonies()
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 2. Filter Langsung (Tanpa Delay)
                val keyword = s.toString().trim()
                filterData(keyword)
            }
        })
    }

    // 3. Logika Filter Lokal (Nama, Instansi, atau Umpan Balik)
    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allTestimoniList
        } else {
            allTestimoniList.filter {
                // Menggunakan ?. dan == true untuk menangani data null
                (it.clientName?.contains(keyword, ignoreCase = true) == true) ||
                        (it.institution?.contains(keyword, ignoreCase = true) == true) ||
                        (it.feedback?.contains(keyword, ignoreCase = true) == true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    // 4. Update UI & Empty State
    private fun updateListUI(list: List<Testimoni>, keyword: String) {
        testimoniAdapter.updateData(list)

        if (list.isEmpty()) {
            rvTestimoni.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                tvEmptyState.text = "Testimoni tidak ditemukan"
            } else {
                tvEmptyState.text = "Belum ada Testimoni"
            }
        } else {
            rvTestimoni.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchTestimonies() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                // 5. Ambil SEMUA data dari server (param null)
                val response = ApiClient.instance.getTestimonies(null)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    // Simpan ke Master List
                    allTestimoniList = listData.sortedByDescending { it.id }

                    // Tampilkan data sesuai pencarian saat ini
                    val currentKeyword = etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TESTIMONI_API", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // ... (Fungsi delete tetap sama) ...
    private fun showDeleteConfirmation(data: Testimoni) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Testimoni")
            .setMessage("Hapus ulasan dari ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
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
                    Toast.makeText(context, "Testimoni Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchTestimonies() // Refresh data
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}