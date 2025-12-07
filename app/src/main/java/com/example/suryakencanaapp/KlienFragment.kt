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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.ClientAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Client
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KlienFragment : Fragment(R.layout.fragment_klien) { // Pastikan nama XML benar

    private lateinit var rvClient: RecyclerView
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var etSearch: EditText
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView (Grid 2 Kolom)
        rvClient = view.findViewById(R.id.rvClient)
        rvClient.layoutManager = GridLayoutManager(context, 2)

        // Init Adapter
        clientAdapter = ClientAdapter(listOf()) { clientToDelete ->
            showDeleteConfirmation(clientToDelete)
        }
        rvClient.adapter = clientAdapter

        // 2. Setup Tombol Tambah
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddClient)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        // 3. Setup Search
        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        fetchClients() // Refresh data saat kembali ke halaman ini
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Batalkan pencarian sebelumnya jika user masih ngetik
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    // Tunggu 800ms (Debounce)
                    delay(800)

                    val keyword = s.toString().trim()
                    if (keyword.isNotEmpty()) {
                        fetchClients(keyword) // Cari: ?search=keyword
                    } else {
                        fetchClients(null) // Kosong: Ambil semua
                    }
                }
            }
        })
    }

    private fun fetchClients(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                // Panggil API
                val response = ApiClient.instance.getClients(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    // Urutkan ID terbesar (terbaru) di atas
                    val sortedList = listData.sortedByDescending { it.id }

                    clientAdapter.updateData(sortedList)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CLIENT_API", "Error: ${e.message}")
            }
        }
    }

    private fun showDeleteConfirmation(data: Client) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Klien")
            .setMessage("Yakin ingin menghapus ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteClientApi(data.id)
            }
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
                    Toast.makeText(context, "Berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    fetchClients() // Refresh list
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}