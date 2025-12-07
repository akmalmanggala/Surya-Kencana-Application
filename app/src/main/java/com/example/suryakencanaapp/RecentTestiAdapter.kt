package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.Testimoni

class RecentTestiAdapter(
    private var testimonialList: List<Testimoni>
) : RecyclerView.Adapter<RecentTestiAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ID ini harus ada di file xml: item_dashboard_testimoni.xml
        val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCompany: TextView = itemView.findViewById(R.id.tvCompany)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate layout item khusus dashboard
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_testimoni, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = testimonialList[position]

        // 1. Set Text Data (Dengan Null Safety)
        holder.tvName.text = data.clientName ?: "Tanpa Nama"
        holder.tvCompany.text = data.institution ?: "-"
        holder.tvDate.text = data.date ?: ""

        // Tambahkan tanda kutip ("...") agar terlihat seperti kutipan/testimoni
        holder.tvContent.text = "\"${data.feedback ?: "Tidak ada ulasan"}\""

        // 2. Logika Inisial Avatar (Pengganti Gambar Produk)
        // Ambil huruf pertama dari nama klien
        val name = data.clientName?.trim()

        if (!name.isNullOrEmpty()) {
            holder.tvInitial.text = name[0].toString().uppercase()
        } else {
            holder.tvInitial.text = "?"
        }
    }

    override fun getItemCount() = testimonialList.size

    // Fungsi update data agar tampilan refresh tanpa buat adapter baru
    fun updateData(newList: List<Testimoni>) {
        this.testimonialList = newList
        notifyDataSetChanged()
    }
}