package com.example.suryakencanaapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.databinding.ItemHeroImageBinding

class AlbumImageAdapter(
    private var uriList: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<AlbumImageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHeroImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHeroImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = uriList[position]

        // Load gambar dengan Glide (Optimasi yang sudah kita bahas sebelumnya)
        Glide.with(holder.itemView.context)
            .load(uri)
            .override(300, 300)
            .thumbnail(0.1f)
            .centerCrop()
            .error(android.R.drawable.stat_notify_error)
            .into(holder.binding.imgHero)

        // --- PERBAIKAN BUG HAPUS ---
        holder.binding.btnDeleteImage.setOnClickListener {
            // Gunakan 'adapterPosition' agar selalu dapat posisi terbaru saat diklik
            val currentPosition = holder.adapterPosition

            // Cek keamanan agar tidak crash jika item sedang animasi dihapus
            if (currentPosition != RecyclerView.NO_POSITION) {
                onRemoveClick(currentPosition)
            }
        }
    }

    override fun getItemCount() = uriList.size

    fun updateData(newUris: List<Uri>) {
        uriList.clear()
        uriList.addAll(newUris)
        notifyDataSetChanged()
    }
}