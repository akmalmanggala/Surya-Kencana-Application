package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.EditClientActivity // Pastikan Activity ini ada
import com.example.suryakencanaapp.databinding.ItemClientBinding
import com.example.suryakencanaapp.model.Client

class ClientAdapter(
    private var clientList: List<Client>,
    private val onDeleteClick: (Client) -> Unit
) : RecyclerView.Adapter<ClientAdapter.ClientViewHolder>() {

    class ClientViewHolder(val binding: ItemClientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val client = clientList[position]

        with(holder.binding) {
            tvClientName.text = client.clientName

            if (!client.logoUrl.isNullOrEmpty()) {
                Glide.with(root.context)
                    .load(client.logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imgLogo)
            } else {
                imgLogo.setImageDrawable(null)
            }

            // --- LOGIKA EDIT (Reusable) ---
            val performEdit = {
                val intent = Intent(root.context, EditClientActivity::class.java)
                intent.putExtra("ID", client.id)
                intent.putExtra("NAME", client.clientName)
                intent.putExtra("LOGO_URL", client.logoUrl)
                root.context.startActivity(intent)
            }

            // 1. Klik Tombol Edit -> Edit
            btnEdit.setOnClickListener { performEdit() }

            // 2. Klik Kartu Klien (Root) -> Edit
            root.setOnClickListener { performEdit() }

            // Klik Tombol Hapus -> Hapus
            btnDelete.setOnClickListener { onDeleteClick(client) }
        }
    }

    override fun getItemCount() = clientList.size

    fun updateData(newList: List<Client>) {
        clientList = newList
        notifyDataSetChanged()
    }
}