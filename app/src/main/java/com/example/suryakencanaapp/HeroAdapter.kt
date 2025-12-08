package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.databinding.ItemHeroImageBinding

class HeroAdapter(
    private var imageUrls: List<String>, // List URL untuk ditampilkan
    private var imagePaths: List<String>, // List Path untuk dihapus
    private val onDeleteClick: (String) -> Unit // Callback kirim Path
) : RecyclerView.Adapter<HeroAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHeroImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHeroImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = imageUrls[position]

        Glide.with(holder.itemView.context)
            .load(url)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
            .into(holder.binding.imgHero)

        holder.binding.btnDeleteImage.setOnClickListener {
            // Pastikan path tersedia di index yang sama
            if (position < imagePaths.size) {
                onDeleteClick(imagePaths[position])
            }
        }
    }

    override fun getItemCount() = imageUrls.size

    fun updateData(newUrls: List<String>, newPaths: List<String>) {
        this.imageUrls = newUrls
        this.imagePaths = newPaths
        notifyDataSetChanged()
    }
}