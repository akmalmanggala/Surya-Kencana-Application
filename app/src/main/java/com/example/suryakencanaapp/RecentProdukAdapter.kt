package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.databinding.ItemRecentProductBinding
import com.example.suryakencanaapp.model.Product
import java.text.NumberFormat
import java.util.Locale

class RecentProdukAdapter(
    private var productList: List<Product>
) : RecyclerView.Adapter<RecentProdukAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecentProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.binding.tvRecentName.text = product.name

        // --- TAMPILKAN DESKRIPSI ---
        holder.binding.tvRecentDesc.text = product.description ?: "Tidak ada deskripsi"

        // Format Rupiah
        val priceDouble = product.price.toDoubleOrNull() ?: 0.0
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        holder.binding.tvRecentPrice.text = formatRupiah.format(priceDouble)

        // Load Gambar (Ganti URL CDN sesuai milik Anda)
        if (!product.imageUrl.isNullOrEmpty()) {
            // LANGSUNG LOAD SAJA (Asumsi Backend selalu kirim https://...)
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.package_2_24dp_ffffff_fill0_wght400_grad0_opsz24)
                .into(holder.binding.imgRecent)
        }
    }

    override fun getItemCount() = productList.size

    fun updateData(newList: List<Product>) {
        this.productList = newList
        notifyDataSetChanged()
    }
}

