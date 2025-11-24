package com.example.suryakencanaapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var productList: MutableList<Product>,
    private val onDeleteClick: (Product) -> Unit, // Callback klik hapus
    private val onEditClick: (Product) -> Unit    // Callback klik edit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvDesc: TextView = itemView.findViewById(R.id.tvProductDesc)
        val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvNoImage: TextView = itemView.findViewById(R.id.tvNoImage)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produk, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.tvName.text = product.name
        holder.tvDesc.text = product.description ?: "Tidak ada deskripsi"

        // 1. FORMAT HARGA (String "20000000.00" -> Double -> Rupiah)
        val priceDouble = product.price.toDoubleOrNull() ?: 0.0
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        holder.tvPrice.text = formatRupiah.format(priceDouble)

        // 2. GAMBAR (Langsung pakai imageUrl dari API)
        if (!product.imageUrl.isNullOrEmpty()) {
            holder.tvNoImage.visibility = View.GONE

            Glide.with(holder.itemView.context)
                .load(product.imageUrl) // URL SUDAH LENGKAP DARI API
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(holder.imgProduct)
        } else {
            holder.tvNoImage.visibility = View.VISIBLE
            holder.imgProduct.setImageDrawable(null)
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(product) }
        holder.btnEdit.setOnClickListener { onEditClick(product) }
    }

    override fun getItemCount() = productList.size

    // Fungsi untuk update data dari Activity/Fragment
    fun updateData(newProducts: List<Product>) {
        productList.clear()
        productList.addAll(newProducts)
        notifyDataSetChanged()
    }
}