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
import com.example.suryakencanaapp.adapter.HistoryAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.History
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RiwayatFragment : Fragment(R.layout.fragment_riwayat) {

    private lateinit var rvHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView

    // List untuk menyimpan semua data (Master Data)
    private var allHistoryList: List<History> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init Views
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        rvHistory = view.findViewById(R.id.rvRiwayat)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        etSearch = view.findViewById(R.id.etSearch)

        rvHistory.layoutManager = LinearLayoutManager(context)

        // 2. Init Adapter
        historyAdapter = HistoryAdapter(
            listOf(),
            onDeleteClick = { history -> showDeleteConfirmation(history) },
            onEditClick = { history ->
                val intent = Intent(context, EditHistoryActivity::class.java)
                intent.putExtra("ID", history.id)
                intent.putExtra("TAHUN", history.tahun.toString())
                intent.putExtra("JUDUL", history.judul)
                intent.putExtra("DESKRIPSI", history.deskripsi)
                intent.putExtra("IMAGE_URL", history.imageUrl)
                startActivity(intent)
            }
        )
        rvHistory.adapter = historyAdapter

        // 3. Listener Refresh
        swipeRefresh.setOnRefreshListener {
            fetchHistories()
        }

        // 4. Tombol Tambah
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddRiwayat)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddHistoryActivity::class.java))
        }

        // 5. Setup Pencarian
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        // Fetch data setiap halaman dibuka (agar data selalu fresh setelah edit/add)
        fetchHistories()
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                filterData(keyword)
            }
        })
    }

    // --- FUNGSI FILTER & PENCARIAN (PERBAIKAN UTAMA) ---
    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allHistoryList
        } else {
            // Cari berdasarkan JUDUL atau TAHUN atau DESKRIPSI
            allHistoryList.filter {
                it.judul.contains(keyword, ignoreCase = true) ||
                        it.tahun.toString().contains(keyword) ||
                        it.deskripsi.contains(keyword, ignoreCase = true)
            }
        }

        // Panggil fungsi update UI agar Empty State juga berubah
        updateListUI(filteredList, keyword)
    }

    // Fungsi Helper untuk mengatur List dan Teks Kosong
    private fun updateListUI(list: List<History>, keyword: String) {
        historyAdapter.updateData(list)

        if (list.isEmpty()) {
            rvHistory.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                tvEmptyState.text = "Riwayat tidak ditemukan"
            } else {
                tvEmptyState.text = "Belum ada Riwayat"
            }
        } else {
            rvHistory.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchHistories() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Ambil semua data
                val response = ApiClient.instance.getHistories()

                if (response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!

                    // Urutkan Tahun Terbaru di Atas (Descending)
                    allHistoryList = rawList.sortedByDescending { it.tahun }

                    // Tampilkan data (sesuai status pencarian saat ini)
                    val currentKeyword = etSearch.text.toString().trim()
                    filterData(currentKeyword)

                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HISTORY", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showDeleteConfirmation(data: History) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Riwayat")
            .setMessage("Yakin ingin menghapus ${data.judul} (${data.tahun})?")
            .setPositiveButton("Hapus") { _, _ -> deleteHistoryApi(data.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteHistoryApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteHistory("Bearer $token", id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Riwayat Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchHistories()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}