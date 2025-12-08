package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.R

class HeroAdapter(
    private var imageUrls: List<String>, // List URL untuk ditampilkan
    private var imagePaths: List<String>, // List Path untuk dihapus
    private val onDeleteClick: (String) -> Unit // Callback kirim Path
) : RecyclerView.Adapter<HeroAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgHero: ImageView = view.findViewById(R.id.imgHero)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hero_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = imageUrls[position]

        Glide.with(holder.itemView.context)
            .load(url)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
            .into(holder.imgHero)

        holder.btnDelete.setOnClickListener {
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