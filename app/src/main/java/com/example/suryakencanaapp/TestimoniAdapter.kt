package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.EditTestimoniActivity
import com.example.suryakencanaapp.databinding.ItemTestimoniBinding
import com.example.suryakencanaapp.model.Testimoni

// LIHAT PERUBAHAN DI SINI (Constructor tambah parameter onDeleteClick)
class TestimoniAdapter(
    private var listTestimoni: List<Testimoni>,
    private val onDeleteClick: (Testimoni) -> Unit // <--- Callback fungsi
) : RecyclerView.Adapter<TestimoniAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTestimoniBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestimoniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listTestimoni[position]

        // ... kode set text & inisial (sama seperti sebelumnya) ...
        holder.binding.tvName.text = data.clientName ?: "Tanpa Nama"
        holder.binding.tvCompany.text = data.institution ?: "-"
        holder.binding.tvContent.text = data.feedback ?: ""
        holder.binding.tvDate.text = data.date ?: ""

        val name = data.clientName?.trim()
        if (!name.isNullOrEmpty()) {
            holder.binding.tvInitial.text = name[0].toString().uppercase()
        } else {
            holder.binding.tvInitial.text = "?"
        }

        // Tombol Edit (Tetap pakai Intent)
        holder.binding.btnEdit.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditTestimoniActivity::class.java)
            intent.putExtra("ID", data.id)
            intent.putExtra("NAME", data.clientName)
            intent.putExtra("INSTITUTION", data.institution)
            intent.putExtra("FEEDBACK", data.feedback)
            intent.putExtra("DATE", data.date)
            holder.itemView.context.startActivity(intent)
        }

        // Tombol Hapus (PANGGIL CALLBACK)
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(data) // <--- Lapor ke Fragment: "Eh, data ini mau dihapus!"
        }
    }

    override fun getItemCount(): Int = listTestimoni.size

    fun updateData(newList: List<Testimoni>) {
        this.listTestimoni = newList
        notifyDataSetChanged()
    }
}