package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.databinding.ItemHistoryBinding
import com.example.suryakencanaapp.model.History

class HistoryAdapter(
    private var historyList: List<History>,
    private val onDeleteClick: (History) -> Unit,
    private val onEditClick: (History) -> Unit // Tambahkan callback edit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = historyList[position]

        // --- SESUAIKAN DENGAN MODEL BARU ---
        holder.binding.tvYearBubble.text = data.tahun.toString()
        holder.binding.tvHistoryTitle.text = data.judul
        holder.binding.tvHistoryDesc.text = data.deskripsi

        // Load Gambar
        if (!data.imageUrl.isNullOrEmpty()) {
            holder.binding.imgHistory.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(data.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
                .centerCrop()
                .into(holder.binding.imgHistory)
        } else {
            // Placeholder default
            holder.binding.imgHistory.visibility = View.GONE
        }

        // Klik Hapus
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(data) }

        // Klik Edit
        holder.binding.btnEdit.setOnClickListener { onEditClick(data) }

        holder.binding.root.setOnClickListener { onEditClick(data) }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<History>) {
        this.historyList = newList
        notifyDataSetChanged()
    }
}