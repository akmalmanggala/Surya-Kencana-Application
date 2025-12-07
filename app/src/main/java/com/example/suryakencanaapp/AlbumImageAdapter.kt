package com.example.suryakencanaapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy // PENTING: Tambahkan Import ini
import com.bumptech.glide.request.RequestOptions // PENTING: Tambahkan Import ini
import com.example.suryakencanaapp.R

class AlbumImageAdapter(
    private var uriList: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<AlbumImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgHero)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hero_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = uriList[position]

        // --- OPTIMASI GLIDE (AGAR TIDAK LEMOT) ---
        Glide.with(holder.itemView.context)
            .load(uri)
            // 1. Override: Paksa ubah ukuran jadi kecil (misal 300x300 pixel)
            // Karena preview di layar HP cuma kecil, ini akan SANGAT mempercepat loading
            .override(300, 300)

            // 2. DiskCache: Simpan hasil download agar kalau dibuka lagi langsung muncul
            .diskCacheStrategy(DiskCacheStrategy.ALL)

            // 3. Thumbnail: Tampilkan versi buram dulu (0.1x ukuran) sambil nunggu yang jelas
            .thumbnail(0.1f)

            .centerCrop()
            .error(android.R.drawable.stat_notify_error)
            .into(holder.img)
        // -----------------------------------------

        holder.btnDelete.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = uriList.size

    fun updateData(newUris: List<Uri>) {
        uriList.clear()
        uriList.addAll(newUris)
        notifyDataSetChanged()
    }
}