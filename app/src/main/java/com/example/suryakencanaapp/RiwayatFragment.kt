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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.HistoryAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentRiwayatBinding
import com.example.suryakencanaapp.model.History
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter
    private var allHistoryList: List<History> = listOf()
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRiwayat.layoutManager = LinearLayoutManager(context)

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
        binding.rvRiwayat.adapter = historyAdapter

        binding.swipeRefresh.setOnRefreshListener {
            fetchHistories()
        }

        binding.btnAddRiwayat.setOnClickListener {
            startActivity(Intent(requireContext(), AddHistoryActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchHistories()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = AlertDialog.Builder(requireContext())
                // Menggunakan layout_loading_dialog.xml yang sudah Anda buat sebelumnya
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false) // User tidak bisa cancel sembarangan
                loadingDialog = builder.create()
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
        } else {
            loadingDialog?.dismiss()
        }
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
            binding.rvRiwayat.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                binding.tvEmptyState.text = "Riwayat tidak ditemukan"
            } else {
                binding.tvEmptyState.text = "Belum ada Riwayat"
            }
        } else {
            binding.rvRiwayat.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchHistories() {
        _binding?.swipeRefresh?.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getHistories()

                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!

                    allHistoryList = rawList.sortedByDescending { it.tahun }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)

                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HISTORY", "Error: ${e.message}")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setLoading(true)
                val response = ApiClient.instance.deleteHistory("Bearer $token", id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Riwayat Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchHistories()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // --- PERBAIKAN POIN 2 ---
                if (e is CancellationException) {
                    // Ignore
                } else {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                setLoading(false)
            }
        }
    }
}