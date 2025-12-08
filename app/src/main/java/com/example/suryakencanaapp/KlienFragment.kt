package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.suryakencanaapp.adapter.ClientAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentKlienBinding
import com.example.suryakencanaapp.model.Client
import kotlinx.coroutines.launch

class KlienFragment : Fragment() {

    private var _binding: FragmentKlienBinding? = null
    private val binding get() = _binding!!
    private lateinit var clientAdapter: ClientAdapter
    private var allClientList: List<Client> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKlienBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvClient.layoutManager = GridLayoutManager(context, 2)

        clientAdapter = ClientAdapter(listOf()) { clientToDelete ->
            showDeleteConfirmation(clientToDelete)
        }
        binding.rvClient.adapter = clientAdapter

        binding.swipeRefresh.setOnRefreshListener {
            fetchClients()
        }

        binding.btnAddClient.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchClients()
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
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
            binding.rvClient.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                binding.tvEmptyState.text = "Klien tidak ditemukan"
            } else {
                binding.tvEmptyState.text = "Belum ada Klien"
            }
        } else {
            binding.rvClient.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchClients() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getClients(null)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    // Simpan ke Master List
                    allClientList = listData.sortedByDescending { it.id }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CLIENT_API", "Error: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
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