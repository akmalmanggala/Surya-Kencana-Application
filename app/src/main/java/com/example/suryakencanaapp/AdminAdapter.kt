package com.example.suryakencanaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.databinding.ItemAdminBinding
import com.example.suryakencanaapp.model.Admin
import java.text.SimpleDateFormat
import java.util.Locale

class AdminAdapter(
    private var adminList: List<Admin>,
    private val onDeleteClick: (Admin) -> Unit
) : RecyclerView.Adapter<AdminAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = adminList[position]

        holder.binding.tvUsername.text = data.username

        // Logika Inisial (Huruf Pertama)
        if (data.username.isNotEmpty()) {
            holder.binding.tvInitial.text = data.username.first().toString().uppercase()
        }

        // Format Tanggal (Opsional: Agar lebih rapi)
        // Input: 2025-12-05T02:09:30.092Z -> Output: 05 Dec 2025
        holder.binding.tvDate.text = formatDate(data.createdAt)

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