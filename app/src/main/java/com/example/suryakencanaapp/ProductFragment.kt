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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.ProdukAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Product
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ProductFragment : Fragment(R.layout.fragment_product) {

    private lateinit var productAdapter: ProdukAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView

    // 1. Simpan Semua Data di sini (Master Data)
    private var allProductList: List<Product> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Listener Refresh
        swipeRefresh.setOnRefreshListener {
            fetchProducts()
        }

        // Cek RecyclerView
        val rvCheck = view.findViewById<RecyclerView>(R.id.rvProducts)
        if (rvCheck == null) {
            Log.e("PRODUK_ERROR", "RecyclerView tidak ditemukan!")
            return
        }
        rvProducts = rvCheck
        rvProducts.layoutManager = GridLayoutManager(context, 2)

        // 2. Setup Adapter (Dengan Logic Edit yang Benar)
        productAdapter = ProdukAdapter(
            mutableListOf(),
            onDeleteClick = { product -> confirmDelete(product) },
            onEditClick = { product ->
                // Pindah ke Halaman Edit
                val intent = Intent(context, EditProdukActivity::class.java)
                intent.putExtra("ID", product.id)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMAGE_URL", product.imageUrl)
                startActivity(intent)
            }
        )
        rvProducts.adapter = productAdapter

        val btnAddProduct = view.findViewById<MaterialButton>(R.id.btnAddProduct)
        btnAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }

        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()
        // Ambil data terbaru setiap masuk halaman
        fetchProducts()
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 3. Filter Langsung (Tanpa Delay)
                val keyword = s.toString().trim()
                filterData(keyword)
            }
        })
    }

    // 4. Fungsi Filter Lokal (Mencari di Nama & Deskripsi)
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

    // 5. Update UI & Empty State
    private fun updateListUI(list: List<Product>, keyword: String) {
        productAdapter.updateData(list)

        if (list.isEmpty()) {
            rvProducts.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE

            if (keyword.isNotEmpty()) {
                tvEmptyState.text = "Produk tidak ditemukan"
            } else {
                tvEmptyState.text = "Belum ada produk"
            }
        } else {
            rvProducts.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun fetchProducts() {
        swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 6. Ambil SEMUA data dari Server (param null)
                val response = ApiClient.instance.getProducts(null)

                if (response.isSuccessful && response.body() != null) {
                    // Simpan ke Master List
                    allProductList = response.body()!!.sortedByDescending { it.id }

                    // Tampilkan data (sesuai pencarian yang sedang diketik)
                    val currentKeyword = etSearch.text.toString().trim()
                    filterData(currentKeyword)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
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
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteProduct("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Produk Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchProducts() // Refresh data
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(context, "Gagal: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}