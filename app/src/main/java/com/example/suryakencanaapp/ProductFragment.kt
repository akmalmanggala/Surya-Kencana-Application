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
import com.example.suryakencanaapp.adapter.ProdukAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentProductBinding // Import Binding Fragment
import com.example.suryakencanaapp.model.Product
import kotlinx.coroutines.launch

class ProductFragment : Fragment() { // Hapus constructor R.layout...

    // 1. Setup Binding Fragment
    private var _binding: FragmentProductBinding? = null
    // Properti ini hanya valid antara onCreateView dan onDestroyView
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProdukAdapter
    private var allProductList: List<Product> = listOf()
    private var loadingDialog: AlertDialog? = null

    // 2. Gunakan onCreateView untuk inflate layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 3. Akses semua view pakai 'binding.'
        binding.swipeRefresh.setOnRefreshListener {
            fetchProducts()
        }

        binding.rvProducts.layoutManager = GridLayoutManager(context, 2)

        productAdapter = ProdukAdapter(
            mutableListOf(),
            onDeleteClick = { product -> confirmDelete(product) },
            onEditClick = { product ->
                val intent = Intent(context, EditProdukActivity::class.java)
                intent.putExtra("ID", product.id)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMAGE_URL", product.imageUrl)
                startActivity(intent)
            }
        )
        binding.rvProducts.adapter = productAdapter

        binding.btnAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }

        setupSearchListener()
    }

    // PENTING: Bersihkan binding saat view hancur untuk hemat memori
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchProducts()
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
            allProductList
        } else {
            allProductList.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                        (it.description?.contains(keyword, ignoreCase = true) == true)||
                        it.price.toString().contains(keyword, ignoreCase = true)
            }
        }
        updateListUI(filteredList, keyword)
    }

    private fun updateListUI(list: List<Product>, keyword: String) {
        productAdapter.updateData(list)

        // Akses Empty State dan Recycler View lewat binding
        if (list.isEmpty()) {
            binding.rvProducts.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                binding.tvEmptyState.text = "Produk tidak ditemukan"
            } else {
                binding.tvEmptyState.text = "Belum ada produk"
            }
        } else {
            binding.rvProducts.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchProducts() {
        _binding?.swipeRefresh?.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProducts(null)

                if (_binding != null && response.isSuccessful && response.body() != null) {
                    allProductList = response.body()!!.sortedByDescending { it.id }

                    val currentKeyword = binding.etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}", e)
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(context)
            .setTitle("Hapus Produk")
            .setMessage("Hapus ${product.name}?")
            .setPositiveButton("Hapus") { _, _ -> deleteProductApi(product.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteProductApi(id: Int) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)
                val response = ApiClient.instance.deleteProduct("Bearer $token", id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Produk Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchProducts()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(context, "Gagal: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }
}