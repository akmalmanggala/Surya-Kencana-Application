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
import com.example.suryakencanaapp.adapter.TestimoniAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentTestimoniBinding
import com.example.suryakencanaapp.model.Testimoni
import kotlinx.coroutines.launch

class TestimoniFragment : Fragment() {

    private var _binding: FragmentTestimoniBinding? = null
    private val binding get() = _binding!!
    private lateinit var testimoniAdapter: TestimoniAdapter
    private var allTestimoniList: List<Testimoni> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestimoniBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTestimoni.layoutManager = LinearLayoutManager(context)

        testimoniAdapter = TestimoniAdapter(listOf()) { testimoniToDelete ->
            showDeleteConfirmation(testimoniToDelete)
        }
        binding.rvTestimoni.adapter = testimoniAdapter

        binding.swipeRefresh.setOnRefreshListener {
            fetchTestimonies()
        }

        binding.btnAddTestimoni.setOnClickListener {
            startActivity(Intent(requireContext(), AddTestimoniActivity::class.java))
        }

        setupSearchListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchTestimonies()
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

    // 3. Logika Filter Lokal (Nama, Instansi, atau Umpan Balik)
    private fun filterData(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allTestimoniList
        } else {
            allTestimoniList.filter {
                // Menggunakan ?. dan == true untuk menangani data null
                (it.clientName?.contains(keyword, ignoreCase = true) == true) ||
                        (it.institution?.contains(keyword, ignoreCase = true) == true) ||
                        (it.feedback?.contains(keyword, ignoreCase = true) == true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    // 4. Update UI & Empty State
    private fun updateListUI(list: List<Testimoni>, keyword: String) {
        testimoniAdapter.updateData(list)

        if (list.isEmpty()) {
            binding.rvTestimoni.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                binding.tvEmptyState.text = "Testimoni tidak ditemukan"
            } else {
                binding.tvEmptyState.text = "Belum ada Testimoni"
            }
        } else {
            binding.rvTestimoni.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchTestimonies() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getTestimonies(null)

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    allTestimoniList = listData.sortedByDescending { it.id }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TESTIMONI_API", "Error: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ... (Fungsi delete tetap sama) ...
    private fun showDeleteConfirmation(data: Testimoni) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Testimoni")
            .setMessage("Hapus ulasan dari ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
                data.id?.let { idPasti ->
                    deleteTestimoniApi(idPasti)
                } ?: run {
                    Toast.makeText(context, "Gagal: ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteTestimoniApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteTestimoni("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Testimoni Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchTestimonies() // Refresh data
                } else {
                    Toast.makeText(context, "Gagal hapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}