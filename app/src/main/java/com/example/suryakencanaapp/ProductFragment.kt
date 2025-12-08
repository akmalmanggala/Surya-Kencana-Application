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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.ProdukAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Product
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ProductFragment : Fragment(R.layout.fragment_product) {

    private lateinit var productAdapter: ProdukAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            fetchProducts()
        }

        // 1. Cek Apakah View Ditemukan (Mencegah NullPointer)
        val rvCheck = view.findViewById<RecyclerView>(R.id.rvProducts)
        if (rvCheck == null) {
            Log.e("PRODUK_ERROR", "RecyclerView dengan ID rvProducts TIDAK DITEMUKAN di XML!")
            return // Stop agar tidak crash
        }
        rvProducts = rvCheck

        // Setup RecyclerView
        rvProducts.layoutManager = GridLayoutManager(context, 2)

        // 2. INIT ADAPTER (Wajib di sini)
        productAdapter = ProdukAdapter(
            mutableListOf(),
            onDeleteClick = { product -> confirmDelete(product) },
            onEditClick = { product ->
                Toast.makeText(context, "Edit ${product.name}", Toast.LENGTH_SHORT).show()
            }
        )
        rvProducts.adapter = productAdapter

        // Init Komponen Lain
        val btnAddProduct = view.findViewById<MaterialButton>(R.id.btnAddProduct)
        btnAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }

        etSearch = view.findViewById(R.id.etSearch)
        setupSearchListener()
    }

    override fun onResume() {
        super.onResume()

        // 1. Cek Keamanan: Pastikan Adapter DAN EditText sudah siap
        if (::productAdapter.isInitialized && ::etSearch.isInitialized) {

            // 2. Ambil kata kunci terakhir (agar pencarian tidak hilang)
            val keyword = etSearch.text.toString().trim()

            // 3. Panggil fetch dengan kata kunci tersebut
            fetchProducts(if (keyword.isNotEmpty()) keyword else null)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                // Gunakan viewLifecycleOwner agar aman saat fragment dihancurkan
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(800)
                    val keyword = s.toString().trim()
                    if (keyword.isNotEmpty()) fetchProducts(keyword) else fetchProducts(null)
                }
            }
        })
    }

    private fun fetchProducts(keyword: String? = null) {
        // PENGAMAN: Jangan lanjut jika adapter belum siap
        if (!::productAdapter.isInitialized) return

        // 4. Tampilkan Loading (Pakai animasi SwipeRefresh)
        swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProducts(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val products = response.body()!!
                    val sortedProduct = products.sortedByDescending { it.id }
                    productAdapter.updateData(sortedProduct)
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}")
            } finally {
                // 5. Matikan Loading (PENTING!)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // --- FUNGSI DELETE (Tetap Sama) ---
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
                // UPDATE: Kirim token ke API
                val response = ApiClient.instance.deleteProduct("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Produk Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                    fetchProducts() // Refresh list otomatis
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