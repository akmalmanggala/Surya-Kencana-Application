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
import kotlin.coroutines.cancellation.CancellationException

class TestimoniFragment : Fragment() {

    private var _binding: FragmentTestimoniBinding? = null
    private val binding get() = _binding!!
    private lateinit var testimoniAdapter: TestimoniAdapter
    private var allTestimoniList: List<Testimoni> = listOf()
    private var loadingDialog: AlertDialog? = null

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
            allTestimoniList
        } else {
            allTestimoniList.filter {
                (it.clientName?.contains(keyword, ignoreCase = true) == true) ||
                        (it.institution?.contains(keyword, ignoreCase = true) == true) ||
                        (it.feedback?.contains(keyword, ignoreCase = true) == true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    private fun updateListUI(list: List<Testimoni>, keyword: String) {
        // Cek _binding agar tidak crash jika fragment sudah tutup
        if (_binding == null) return

        testimoniAdapter.updateData(list)

        if (list.isEmpty()) {
            binding.rvTestimoni.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = if (keyword.isNotEmpty()) "Testimoni tidak ditemukan" else "Belum ada Testimoni"
        } else {
            binding.rvTestimoni.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchTestimonies() {
        // Gunakan Safe Call (?)
        _binding?.swipeRefresh?.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getTestimonies(null)

                // Cek _binding != null sebelum update UI
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!
                    allTestimoniList = listData.sortedByDescending { it.id }

                    val currentKeyword = _binding?.etSearch?.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    if (_binding != null) {
                        Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TESTIMONI_API", "Error: ${e.message}")
            } finally {
                // PENTING: Gunakan _binding? agar tidak crash di blok finally
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showDeleteConfirmation(data: Testimoni) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Testimoni")
            .setMessage("Hapus ulasan dari ${data.clientName}?")
            .setPositiveButton("Hapus") { _, _ ->
                data.id?.let { idPasti ->
                    deleteTestimoniApi(idPasti)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteTestimoniApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)

                val response = ApiClient.instance.deleteTestimoni("Bearer $token", id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Testimoni Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchTestimonies()
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