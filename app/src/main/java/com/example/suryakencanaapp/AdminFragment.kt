package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.EditText
import android.text.TextWatcher
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.suryakencanaapp.adapter.AdminAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


class AdminFragment : Fragment(R.layout.fragment_admin) {

    private lateinit var rvAdmin: RecyclerView
    private lateinit var adminAdapter: AdminAdapter
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAdmin = view.findViewById(R.id.rvAdmin)
        rvAdmin.layoutManager = LinearLayoutManager(context)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            fetchAdmins()
        }
        // Init Adapter
        adminAdapter = AdminAdapter(listOf()) {
        }
        rvAdmin.adapter = adminAdapter

        // Tombol Tambah
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddAdmin) // Pastikan ID di XML benar (misal btnAddAdmin)
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddAdminActivity::class.java))
        }

        etSearch = view.findViewById(R.id.etSearch) // Pastikan ID di XML benar
        setupSearchListener()

        // Fetch Data
        fetchAdmins()
    }

    override fun onResume() {
        super.onResume()

        // 1. Cek Keamanan: Pastikan Adapter DAN EditText sudah siap
        if (::adminAdapter.isInitialized && ::etSearch.isInitialized) {

            // 2. Ambil kata kunci terakhir (agar pencarian tidak hilang)
            val keyword = etSearch.text.toString().trim()

            // 3. Panggil fetch dengan kata kunci tersebut
            fetchAdmins(if (keyword.isNotEmpty()) keyword else null)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Batalkan pencarian sebelumnya
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    // Tunggu 800ms agar tidak spam server
                    delay(800)

                    val keyword = s.toString().trim()
                    if (keyword.isNotEmpty()) {
                        fetchAdmins(keyword) // Cari dengan kata kunci
                    } else {
                        fetchAdmins(null) // Ambil semua
                    }
                }
            }
        })
    }

    private fun fetchAdmins(keyword: String? = null) {
        swipeRefresh.isRefreshing = true
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getAdmins("Bearer $token", keyword)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!
                    // Sorting terbaru diatas
                    val sortedList = listData.sortedByDescending { it.id }
                    adminAdapter.updateData(sortedList)
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

//    private fun showDeleteConfirmation(admin: Admin) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Hapus Admin")
//            .setMessage("Yakin ingin menghapus admin '${admin.username}'?")
//            .setPositiveButton("Hapus") { _, _ -> deleteAdminApi(admin.id) }
//            .setNegativeButton("Batal", null)
//            .show()
//    }

//    private fun deleteAdminApi(id: Int) {
//        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
//        val token = prefs.getString("TOKEN", "") ?: return
//
//        lifecycleScope.launch {
//            try {
//                val response = ApiClient.instance.deleteAdmin("Bearer $token", id)
//                if (response.isSuccessful) {
//                    Toast.makeText(context, "Admin dihapus!", Toast.LENGTH_SHORT).show()
//                    fetchAdmins()
//                } else {
//                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
}