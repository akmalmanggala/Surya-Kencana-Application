package com.example.suryakencanaapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.model.Testimoni

// LIHAT PERUBAHAN DI SINI (Constructor tambah parameter onDeleteClick)
class TestimoniAdapter(
    private var listTestimoni: List<Testimoni>,
    private val onDeleteClick: (Testimoni) -> Unit // <--- Callback fungsi
) : RecyclerView.Adapter<TestimoniAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ... deklarasi view (sama seperti sebelumnya) ...
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvCompany: TextView = view.findViewById(R.id.tvCompany)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_testimoni, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listTestimoni[position]

        // ... kode set text & inisial (sama seperti sebelumnya) ...
        holder.tvName.text = data.clientName ?: "Tanpa Nama"
        holder.tvCompany.text = data.institution ?: "-"
        holder.tvContent.text = data.feedback ?: ""
        holder.tvDate.text = data.date ?: ""

        val name = data.clientName?.trim()
        if (!name.isNullOrEmpty()) {
            holder.tvInitial.text = name[0].toString().uppercase()
        } else {
            holder.tvInitial.text = "?"
        }

        // Tombol Edit (Tetap pakai Intent)
        holder.btnEdit.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditTestimoniActivity::class.java)
            intent.putExtra("ID", data.id)
            intent.putExtra("NAME", data.clientName)
            intent.putExtra("INSTITUTION", data.institution)
            intent.putExtra("FEEDBACK", data.feedback)
            intent.putExtra("DATE", data.date)
            holder.itemView.context.startActivity(intent)
        }

        // Tombol Hapus (PANGGIL CALLBACK)
        holder.btnDelete.setOnClickListener {
            onDeleteClick(data) // <--- Lapor ke Fragment: "Eh, data ini mau dihapus!"
        }
    }

    override fun getItemCount(): Int = listTestimoni.size

    fun updateData(newList: List<Testimoni>) {
        this.listTestimoni = newList
        notifyDataSetChanged()
    }
}