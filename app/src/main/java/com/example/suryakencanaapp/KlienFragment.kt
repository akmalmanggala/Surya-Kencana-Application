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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // Import ini
import com.example.suryakencanaapp.adapter.ClientAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Client
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KlienFragment : Fragment(R.layout.fragment_klien) {

    private lateinit var rvClient: RecyclerView
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout // Tambahan Variabel
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init SwipeRefresh
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // 2. Setup RecyclerView
        rvClient = view.findViewById(R.id.rvClient)
        rvClient.layoutManager = GridLayoutManager(context, 2)

        clientAdapter = ClientAdapter(listOf()) { clientToDelete ->
            showDeleteConfirmation(clientToDelete)
        }
        rvClient.adapter = clientAdapter

        // 3. Listener Manual Refresh (Tarik ke bawah)
        swipeRefresh.setOnRefreshListener {
            val keyword = etSearch.text.toString().trim()
            fetchClients(if (keyword.isNotEmpty()) keyword else null)
        }

        // 4. Setup Tombol & Search
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddClient)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()

        // JANGAN PANGGIL fetchClients() DI SINI (Biar tidak dobel dengan onResume)
    }

    // 5. Load Otomatis saat masuk halaman
    override fun onResume() {
        super.onResume()

        // 1. Cek Keamanan: Pastikan Adapter DAN EditText sudah siap
        if (::clientAdapter.isInitialized && ::etSearch.isInitialized) {

            // 2. Ambil kata kunci terakhir (agar pencarian tidak hilang)
            val keyword = etSearch.text.toString().trim()

            // 3. Panggil fetch dengan kata kunci tersebut
            fetchClients(if (keyword.isNotEmpty()) keyword else null)
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
                        fetchClients(keyword)
                    } else {
                        fetchClients(null)
                    }
                }
            }
        })
    }

    private fun fetchClients(keyword: String? = null) {
        // Tampilkan loading (putar-putar)
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getClients(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!
                    val sortedList = listData.sortedByDescending { it.id }
                    clientAdapter.updateData(sortedList)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CLIENT_API", "Error: ${e.message}")
            } finally {
                // Matikan loading (PENTING)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // ... (Fungsi showDeleteConfirmation dan deleteClientApi SAMA SAJA) ...
    // Pastikan deleteClientApi memanggil fetchClients() agar list ter-refresh setelah hapus
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
                    Toast.makeText(context, "Klien Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchClients() // Refresh list otomatis (spinner akan muncul sebentar)
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}