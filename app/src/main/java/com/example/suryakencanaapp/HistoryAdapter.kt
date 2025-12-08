package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.History

class HistoryAdapter(
    private var historyList: List<History>,
    private val onDeleteClick: (History) -> Unit,
    private val onEditClick: (History) -> Unit // Tambahkan callback edit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvYear: TextView = view.findViewById(R.id.tvYearBubble)
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvHistoryDesc)
        val imgHistory: ImageView = view.findViewById(R.id.imgHistory)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = historyList[position]

        // --- SESUAIKAN DENGAN MODEL BARU ---
        holder.tvYear.text = data.tahun.toString()
        holder.tvTitle.text = data.judul
        holder.tvDesc.text = data.deskripsi

        // Load Gambar
        if (!data.imageUrl.isNullOrEmpty()) {
            holder.imgHistory.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(data.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
                .centerCrop()
                .into(holder.imgHistory)
        } else {
            // Placeholder default
            holder.imgHistory.visibility = View.GONE
        }

        // Klik Hapus
        holder.btnDelete.setOnClickListener { onDeleteClick(data) }

        // Klik Edit
        holder.btnEdit.setOnClickListener { onEditClick(data) }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<History>) {
        this.historyList = newList
        notifyDataSetChanged()
    }
}