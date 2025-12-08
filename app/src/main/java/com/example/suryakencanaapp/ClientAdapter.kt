package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.EditClientActivity
import com.example.suryakencanaapp.databinding.ItemClientBinding
import com.example.suryakencanaapp.model.Client


class ClientAdapter(
    private var clientList: List<Client>,
    private val onDeleteClick: (Client) -> Unit // Callback Delete
) : RecyclerView.Adapter<ClientAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemClientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = clientList[position]

        holder.binding.tvClientName.text = data.clientName
        holder.binding.tvInstitution.text = data.institution ?: "-"

        if (!data.logoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(data.logoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
                .into(holder.binding.imgLogo)
        }

        // Klik Hapus
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(data)
        }

        // Klik Edit (Nanti dibuat Activity Edit-nya, sementara Toast dulu atau Intent kosong)
        holder.binding.btnEdit.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditClientActivity::class.java)
            intent.putExtra("ID", data.id)
            intent.putExtra("NAME", data.clientName)
            intent.putExtra("INSTITUTION", data.institution)
            intent.putExtra("LOGO_URL", data.logoUrl) // Kirim URL logo lama buat preview
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = clientList.size

    fun updateData(newList: List<Client>) {
        this.clientList = newList
        notifyDataSetChanged()
    }
}