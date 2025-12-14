package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.EditTestimoniActivity
import com.example.suryakencanaapp.databinding.ItemTestimoniBinding
import com.example.suryakencanaapp.model.Testimoni

class TestimoniAdapter(
    private var listTestimoni: List<Testimoni>,
    private val onDeleteClick: (Testimoni) -> Unit
) : RecyclerView.Adapter<TestimoniAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTestimoniBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestimoniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listTestimoni[position]

        // Set Data Teks
        holder.binding.tvName.text = data.clientName ?: "Tanpa Nama"
        holder.binding.tvCompany.text = data.institution ?: "-"
        holder.binding.tvContent.text = data.feedback ?: ""
        holder.binding.tvDate.text = data.date ?: ""

        // Set Inisial
        val name = data.clientName?.trim()
        if (!name.isNullOrEmpty()) {
            holder.binding.tvInitial.text = name[0].toString().uppercase()
        } else {
            holder.binding.tvInitial.text = "?"
        }

        // --- LOGIKA EDIT (Disimpan dalam variabel agar bisa dipakai 2x) ---
        val performEdit = {
            val intent = Intent(holder.itemView.context, EditTestimoniActivity::class.java)
            intent.putExtra("ID", data.id)
            intent.putExtra("NAME", data.clientName)
            intent.putExtra("INSTITUTION", data.institution)
            intent.putExtra("FEEDBACK", data.feedback)
            intent.putExtra("DATE", data.date)
            holder.itemView.context.startActivity(intent)
        }

        // 1. Klik Tombol Edit (Pensil) -> Edit
        holder.binding.btnEdit.setOnClickListener { performEdit() }

        // 2. Klik Kartu Utama -> Edit (REQ ANDA)
        holder.binding.root.setOnClickListener { performEdit() }

        // 3. Klik Tombol Hapus -> Hapus
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(data)
        }
    }

    override fun getItemCount(): Int = listTestimoni.size

    fun updateData(newList: List<Testimoni>) {
        this.listTestimoni = newList
        notifyDataSetChanged()
    }
}