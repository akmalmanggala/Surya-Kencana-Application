package com.example.suryakencanaapp

import android.app.AlertDialog
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

class ProductFragment : Fragment(R.layout.fragment_product) {

    private lateinit var productAdapter: ProdukAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var etSearch: EditText
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        // Panggil fetch hanya jika adapter sudah siap
        if (::productAdapter.isInitialized) {
            fetchProducts()
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
        Log.d("DEBUG_PRODUK", "Memanggil API dengan keyword: $keyword")

        // PENGAMAN: Jangan lanjut jika adapter belum siap
        if (!::productAdapter.isInitialized) {
            Log.e("DEBUG_PRODUK", "Adapter belum siap, batalkan fetch.")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProducts(keyword)

                if (response.isSuccessful && response.body() != null) {
                    val products = response.body()!!
                    Log.d("DEBUG_PRODUK", "Data masuk: ${products.size} item")

                    val sortedProduct = products.sortedByDescending { it.id }
                    productAdapter.updateData(sortedProduct)

                } else {
                    Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}")
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.deleteProduct(id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                    fetchProducts()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}