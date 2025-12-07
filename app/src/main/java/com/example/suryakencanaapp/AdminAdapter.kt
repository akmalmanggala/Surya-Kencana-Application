package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.Admin
import java.text.SimpleDateFormat
import java.util.Locale

class AdminAdapter(
    private var adminList: List<Admin>,
    private val onDeleteClick: (Admin) -> Unit
) : RecyclerView.Adapter<AdminAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        // val btnEdit: ImageButton = view.findViewById(R.id.btnEdit) // Biasanya admin tidak diedit, tapi dihapus
        val btnDelete: ImageButton? = view.findViewById(R.id.btnDelete) // Jika ada tombol hapus di XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = adminList[position]

        holder.tvUsername.text = data.username

        // Logika Inisial (Huruf Pertama)
        if (data.username.isNotEmpty()) {
            holder.tvInitial.text = data.username.first().toString().uppercase()
        }

        // Format Tanggal (Opsional: Agar lebih rapi)
        // Input: 2025-12-05T02:09:30.092Z -> Output: 05 Dec 2025
        holder.tvDate.text = formatDate(data.createdAt)

        // Klik Hapus (Pastikan di XML item_admin ada tombol delete, misal ID btnDelete)
        // Jika tidak ada tombol delete di card, bisa pakai onLongClick di itemView
        holder.itemView.setOnClickListener {
            // Opsional: Klik card untuk detail/hapus
        }

        // Jika Anda menambahkan tombol delete di layout XML item_admin:
        /*
        holder.btnDelete?.setOnClickListener {
            onDeleteClick(data)
        }
        */
    }

    override fun getItemCount() = adminList.size

    fun updateData(newList: List<Admin>) {
        this.adminList = newList
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = parser.parse(dateString)
            "Dibuat: " + (formatter.format(date!!) ?: dateString)
        } catch (e: Exception) {
            "Dibuat: $dateString"
        }
    }
}