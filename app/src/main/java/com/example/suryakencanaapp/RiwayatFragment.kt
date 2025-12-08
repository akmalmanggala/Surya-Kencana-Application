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
    // Simpan semua data asli untuk keperluan filter manual
    private var allHistoryList: List<History> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        rvHistory = view.findViewById(R.id.rvRiwayat)
        rvHistory.layoutManager = LinearLayoutManager(context)

        // Init Adapter (Handle Delete & Edit)
        historyAdapter = HistoryAdapter(
            listOf(),
            onDeleteClick = { history -> showDeleteConfirmation(history) },
            onEditClick = { history ->
                // Navigate to Edit Activity
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

        swipeRefresh.setOnRefreshListener {
            fetchHistories()
        }

        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddRiwayat)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddHistoryActivity::class.java))
        }

        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()

        // 1. Cek Keamanan: Pastikan Adapter DAN EditText sudah siap
        if (::historyAdapter.isInitialized && ::etSearch.isInitialized) {

            // 2. Ambil kata kunci terakhir (agar pencarian tidak hilang)
            val keyword = etSearch.text.toString().trim()

            // 3. Panggil fetch dengan kata kunci tersebut
            fetchHistories(if (keyword.isNotEmpty()) keyword else null)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim().lowercase()
                filterData(keyword)
            }
        })
    }

    // Filter Manual di Android (Karena API tidak support search)
    private fun filterData(keyword: String) {
        if (keyword.isEmpty()) {
            historyAdapter.updateData(allHistoryList)
        } else {
            val filteredList = allHistoryList.filter {
                it.judul.lowercase().contains(keyword) ||
                        it.tahun.toString().contains(keyword)
            }
            historyAdapter.updateData(filteredList)
        }
    }

    private fun fetchHistories(keyword: String? = null) {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Panggil API (Tanpa parameter search)
                val response = ApiClient.instance.getHistories()

                if (response.isSuccessful && response.body() != null) {
                    allHistoryList = response.body()!!

                    // Urutkan Tahun Terlama ke Terbaru (Ascending) sesuai Controller
                    // Atau descending jika ingin terbaru diatas
                    val sortedList = allHistoryList.sortedByDescending { it.tahun }
                    allHistoryList = sortedList // Update master list

                    historyAdapter.updateData(allHistoryList)
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

    // ... (Fungsi showDeleteConfirmation & deleteHistoryApi sama seperti sebelumnya) ...
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