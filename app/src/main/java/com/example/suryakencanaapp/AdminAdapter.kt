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
    private val onDeleteClick: (Admin) -> Unit,
    private val onEditClick: (Admin) -> Unit
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

        holder.binding.tvDate.text = formatDate(data.createdAt)

        // 1. KLIK TOMBOL HAPUS
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(data)
        }

        // 2. KLIK TOMBOL EDIT
        holder.binding.btnEdit.setOnClickListener {
            onEditClick(data)
        }

        // 3. KLIK KARTU (Opsional: Edit juga)
        holder.binding.root.setOnClickListener {
            onEditClick(data)
        }
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