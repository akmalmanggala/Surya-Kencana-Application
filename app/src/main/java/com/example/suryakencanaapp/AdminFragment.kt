package com.example.suryakencanaapp

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
import com.example.suryakencanaapp.adapter.AdminAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Admin
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AdminFragment : Fragment(R.layout.fragment_admin) {

    private lateinit var rvAdmin: RecyclerView
    private lateinit var adminAdapter: AdminAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView

    // 1. Variabel Master Data
    private var allAdminList: List<Admin> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAdmin = view.findViewById(R.id.rvAdmin)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        etSearch = view.findViewById(R.id.etSearch)

        rvAdmin.layoutManager = LinearLayoutManager(context)

        // Init Adapter
        adminAdapter = AdminAdapter(listOf()) {
            // Callback delete/edit jika ada
        }
        rvAdmin.adapter = adminAdapter

        // Listener Refresh
        swipeRefresh.setOnRefreshListener {
            fetchAdmins()
        }

        // Tombol Tambah
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddAdmin)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddAdminActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        fetchAdmins()
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 2. Filter Langsung
                val keyword = s.toString().trim()
                filterData(keyword)
            }
        })
    }

    // 3. Logika Filter Lokal
    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allAdminList
        } else {
            // Filter berdasarkan Username
            allAdminList.filter {
                it.username.contains(keyword, ignoreCase = true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    // 4. Update UI & Empty State
    private fun updateListUI(list: List<Admin>, keyword: String) {
        adminAdapter.updateData(list)

        if (list.isEmpty()) {
            rvAdmin.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                tvEmptyState.text = "Admin tidak ditemukan"
            } else {
                tvEmptyState.text = "Belum ada Admin"
            }
        } else {
            rvAdmin.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchAdmins() {
        swipeRefresh.isRefreshing = true
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                // 5. Ambil SEMUA data (keyword null)
                val response = ApiClient.instance.getAdmins("Bearer $token", null)

                if (response.isSuccessful && response.body() != null) {
                    // Simpan ke Master List
                    allAdminList = response.body()!!.sortedByDescending { it.id }

                    // Tampilkan sesuai pencarian saat ini
                    val currentKeyword = etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ADMIN_API", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

}