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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.suryakencanaapp.adapter.ClientAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Client
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class KlienFragment : Fragment(R.layout.fragment_klien) {

    private lateinit var rvClient: RecyclerView
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView

    // 1. Master Data List
    private var allClientList: List<Client> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        rvClient = view.findViewById(R.id.rvClient)
        etSearch = view.findViewById(R.id.etSearch)

        rvClient.layoutManager = GridLayoutManager(context, 2)

        clientAdapter = ClientAdapter(listOf()) { clientToDelete ->
            showDeleteConfirmation(clientToDelete)
        }
        rvClient.adapter = clientAdapter

        // Listener Refresh
        swipeRefresh.setOnRefreshListener {
            fetchClients()
        }

        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddClient)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        fetchClients()
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

    // 3. Logika Filter Lokal (Nama Klien / Instansi)
    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allClientList
        } else {
            // PERBAIKAN: Tambahkan `?` dan `== true` untuk menangani data null
            allClientList.filter {
                (it.clientName?.contains(keyword, ignoreCase = true) == true) ||
                        (it.institution?.contains(keyword, ignoreCase = true) == true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    // 4. Update UI & Empty State
    private fun updateListUI(list: List<Client>, keyword: String) {
        clientAdapter.updateData(list)

        if (list.isEmpty()) {
            rvClient.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                tvEmptyState.text = "Klien tidak ditemukan"
            } else {
                tvEmptyState.text = "Belum ada Klien"
            }
        } else {
            rvClient.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchClients() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                // 5. Ambil SEMUA data (param null)
                val response = ApiClient.instance.getClients(null)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    // Simpan ke Master List
                    allClientList = listData.sortedByDescending { it.id }

                    // Tampilkan data sesuai search bar saat ini
                    val currentKeyword = etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CLIENT_API", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // ... (Fungsi Delete tetap sama) ...
    private fun showDeleteConfirmation(data: Client) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Klien")
            .setMessage("Yakin ingin menghapus ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ -> deleteClientApi(data.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteClientApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteClient("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Klien Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchClients() // Refresh data
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}