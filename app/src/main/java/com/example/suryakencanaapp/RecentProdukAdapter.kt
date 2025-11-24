package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.Product
import java.text.NumberFormat
import java.util.Locale

class RecentProductAdapter(
    private var productList: List<Product>
) : RecyclerView.Adapter<RecentProductAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRecentName)
        val tvDesc: TextView = itemView.findViewById(R.id.tvRecentDesc) // ID Baru
        val tvPrice: TextView = itemView.findViewById(R.id.tvRecentPrice)
        val img: ImageView = itemView.findViewById(R.id.imgRecent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.tvName.text = product.name

        // --- TAMPILKAN DESKRIPSI ---
        holder.tvDesc.text = product.description ?: "Tidak ada deskripsi"

        // Format Rupiah
        val priceDouble = product.price.toDoubleOrNull() ?: 0.0
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        holder.tvPrice.text = formatRupiah.format(priceDouble)

        // Load Gambar (Ganti URL CDN sesuai milik Anda)
        val cdnBaseUrl = "http://10.0.2.2:8000/storage/"

        if (!product.imageUrl.isNullOrEmpty()) {
            val fullUrl = if (product.imageUrl.startsWith("http")) product.imageUrl else cdnBaseUrl + product.imageUrl

            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .centerCrop()
                .placeholder(R.drawable.package_2_24dp_ffffff_fill0_wght400_grad0_opsz24) // Placeholder saat loading
                .into(holder.img)
        }
    }

    override fun getItemCount() = productList.size
}