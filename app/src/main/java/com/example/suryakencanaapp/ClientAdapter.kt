package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.EditClientActivity
import com.example.suryakencanaapp.R
import com.example.suryakencanaapp.model.Client


class ClientAdapter(
    private var clientList: List<Client>,
    private val onDeleteClick: (Client) -> Unit // Callback Delete
) : RecyclerView.Adapter<ClientAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgLogo: ImageView = view.findViewById(R.id.imgLogo)
        val tvName: TextView = view.findViewById(R.id.tvClientName)
        val tvInstitution: TextView = view.findViewById(R.id.tvInstitution)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_client, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = clientList[position]

        holder.tvName.text = data.clientName
        holder.tvInstitution.text = data.institution ?: "-"

        if (!data.logoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(data.logoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // <--- PENTING: Simpan semua versi
                .into(holder.imgLogo)
        }

        // Klik Hapus
        holder.btnDelete.setOnClickListener {
            onDeleteClick(data)
        }

        // Klik Edit (Nanti dibuat Activity Edit-nya, sementara Toast dulu atau Intent kosong)
        holder.btnEdit.setOnClickListener {
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