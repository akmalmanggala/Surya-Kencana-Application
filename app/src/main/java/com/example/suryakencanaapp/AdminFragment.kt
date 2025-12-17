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
import com.example.suryakencanaapp.adapter.AdminAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentAdminBinding
import com.example.suryakencanaapp.model.Admin
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var adminAdapter: AdminAdapter
    private var allAdminList: List<Admin> = listOf()
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAdmin.layoutManager = LinearLayoutManager(context)

        // --- INIT ADAPTER (PERBAIKAN SINTAKS) ---
        adminAdapter = AdminAdapter(
            listOf(),
            onDeleteClick = { admin -> showDeleteConfirmation(admin) },
            onEditClick = { admin ->
                val intent = Intent(context, EditAdminActivity::class.java)
                intent.putExtra("ID", admin.id)
                intent.putExtra("USERNAME", admin.username)
                startActivity(intent)
            }
        )
        binding.rvAdmin.adapter = adminAdapter

        binding.swipeRefresh.setOnRefreshListener {
            fetchAdmins()
        }

        binding.btnAddAdmin.setOnClickListener {
            startActivity(Intent(requireContext(), AddAdminActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchAdmins()
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

    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allAdminList
        } else {
            allAdminList.filter {
                it.username.contains(keyword, ignoreCase = true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    private fun updateListUI(list: List<Admin>, keyword: String) {
        adminAdapter.updateData(list)

        if (list.isEmpty()) {
            binding.rvAdmin.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = if (keyword.isNotEmpty()) "Admin tidak ditemukan" else "Belum ada Admin"
        } else {
            binding.rvAdmin.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchAdmins() {
        _binding?.swipeRefresh?.isRefreshing = true
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                // Ambil semua data (null) lalu filter lokal
                val response = ApiClient.instance.getAdmins("Bearer $token", null)

                if (_binding != null && response.isSuccessful && response.body() != null) {
                    allAdminList = response.body()!!.sortedByDescending { it.id }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ADMIN_API", "Error: ${e.message}")
            } finally {
                // Gunakan safe call karena view mungkin sudah hancur
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showDeleteConfirmation(data: Admin) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Admin")
            .setMessage("Yakin ingin menghapus ${data.username}?")
            .setPositiveButton("Hapus") { _, _ -> deleteAdminApi(data.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAdminApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)
                val response = ApiClient.instance.deleteAdmin("Bearer $token", id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Admin Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchAdmins()
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
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