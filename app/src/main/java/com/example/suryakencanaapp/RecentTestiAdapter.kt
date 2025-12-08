package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.databinding.ItemRecentTestimoniBinding
import com.example.suryakencanaapp.model.Testimoni

class RecentTestiAdapter(
    private var testimonialList: List<Testimoni>
) : RecyclerView.Adapter<RecentTestiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecentTestimoniBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate layout item khusus dashboard
        val binding = ItemRecentTestimoniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = testimonialList[position]

        // 1. Set Text Data (Dengan Null Safety)
        holder.binding.tvName.text = data.clientName ?: "Tanpa Nama"
        holder.binding.tvCompany.text = data.institution ?: "-"
        holder.binding.tvDate.text = data.date ?: ""

        // Tambahkan tanda kutip ("...") agar terlihat seperti kutipan/testimoni
        holder.binding.tvContent.text = "\"${data.feedback ?: "Tidak ada ulasan"}\""

        // 2. Logika Inisial Avatar (Pengganti Gambar Produk)
        // Ambil huruf pertama dari nama klien
        val name = data.clientName?.trim()

        if (!name.isNullOrEmpty()) {
            holder.binding.tvInitial.text = name[0].toString().uppercase()
        } else {
            holder.binding.tvInitial.text = "?"
        }
    }

    override fun getItemCount() = testimonialList.size

    // Fungsi update data agar tampilan refresh tanpa buat adapter baru
    fun updateData(newList: List<Testimoni>) {
        this.testimonialList = newList
        notifyDataSetChanged()
    }
}