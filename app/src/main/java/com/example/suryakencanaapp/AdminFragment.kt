package com.example.suryakencanaapp

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

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var adminAdapter: AdminAdapter
    private var allAdminList: List<Admin> = listOf()

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

        adminAdapter = AdminAdapter(listOf()) { }
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

    private fun updateListUI(list: List<Admin>, keyword: String) {
        adminAdapter.updateData(list)

        if (list.isEmpty()) {
            binding.rvAdmin.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                binding.tvEmptyState.text = "Admin tidak ditemukan"
            } else {
                binding.tvEmptyState.text = "Belum ada Admin"
            }
        } else {
            binding.rvAdmin.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchAdmins() {
        binding.swipeRefresh.isRefreshing = true
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getAdmins("Bearer $token", null)

                if (response.isSuccessful && response.body() != null) {
                    allAdminList = response.body()!!.sortedByDescending { it.id }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ADMIN_API", "Error: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

}