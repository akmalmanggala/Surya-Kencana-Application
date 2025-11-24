package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.ProductAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Product
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProductFragment : Fragment() {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var etSearch: EditText

    private var searchJob: Job? = null

    // Simpan list original untuk backup saat search kosong
    private var allProducts = listOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        rvProducts = view.findViewById(R.id.rvProducts)
        rvProducts.layoutManager = GridLayoutManager(context, 2) // Grid 2 Kolom

        productAdapter = ProductAdapter(
            mutableListOf(),
            onDeleteClick = { product -> confirmDelete(product) },
            onEditClick = { product ->
                Toast.makeText(context, "Edit ${product.name}", Toast.LENGTH_SHORT).show()
                // Nanti kita arahkan ke halaman EditActivity di sini
            }
        )
        rvProducts.adapter = productAdapter

        // 2. Setup Tombol Tambah
        val btnAddProduct = view.findViewById<MaterialButton>(R.id.btnAddProduct)
        btnAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }

        // 3. Setup Fitur Search
        etSearch = view.findViewById(R.id.etSearch)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 1. Batalkan request sebelumnya jika user masih mengetik
                searchJob?.cancel()

                // 2. Mulai timer baru
                searchJob = lifecycleScope.launch {
                    // Tunggu 500ms (setengah detik) sebelum request
                    delay(800)

                    val keyword = s.toString().trim()

                    if (keyword.isNotEmpty()) {
                        // Jika ada kata kunci, cari ke API
                        fetchProducts(keyword)
                    } else {
                        // Jika kosong, panggil fetchProducts() tanpa param (ambil semua)
                        fetchProducts(null)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Panggil data setiap kali fragment muncul (agar update setelah tambah data)
        fetchProducts()
    }

    // --- FUNGSI API GET PRODUCTS ---
    private fun fetchProducts(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProducts(keyword)

                if (response.isSuccessful && response.body() != null) {
                    // KARENA LANGSUNG LIST, TIDAK PERLU .data
                    val products = response.body()!!

                    // Update List
                    allProducts = products
                    productAdapter.updateData(products)

                    // Logging Sukses
                    android.util.Log.d("API_SUCCESS", "Data masuk: ${products.size} produk")
                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // --- FUNGSI CONFIRM & DELETE ---
    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(context)
            .setTitle("Hapus Produk")
            .setMessage("Anda yakin ingin menghapus ${product.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteProductApi(product.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteProductApi(id: Int) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("DELETE_CHECK", "Menghapus ID: $id")

                val response = ApiClient.instance.deleteProduct(id)

                // Cek status code di Logcat
                android.util.Log.d("DELETE_CHECK", "Response Code: ${response.code()}")

                // 200 = OK, 204 = No Content (Sukses juga)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()

                    // REFRESH LIST SETELAH HAPUS
                    fetchProducts()
                } else {
                    // Baca error message dari server
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    android.util.Log.e("DELETE_CHECK", "Gagal: $errorMsg")
                    Toast.makeText(context, "Gagal menghapus: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DELETE_CHECK", "Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}