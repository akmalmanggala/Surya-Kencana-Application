package com.example.suryakencanaapp.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.R
import com.google.android.material.textfield.TextInputEditText

class MisiAdapter(
    private var misiList: MutableList<String> // List String (Poin-poin misi)
) : RecyclerView.Adapter<MisiAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etMisiItem: TextInputEditText = itemView.findViewById(R.id.etMisiItem)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteMisi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_misi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 1. Isi teks (tanpa memicu TextWatcher berulang kali)
        holder.etMisiItem.setText(misiList[position])

        // 2. Simpan perubahan teks ke dalam List saat user mengetik
        holder.etMisiItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                misiList[holder.adapterPosition] = s.toString() // Update data di list
            }
        })

        // 3. Tombol Hapus
        holder.btnDelete.setOnClickListener {
            misiList.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, misiList.size)
        }
    }

    override fun getItemCount() = misiList.size

    // Fungsi Tambah Poin Kosong
    fun addEmptyItem() {
        misiList.add("")
        notifyItemInserted(misiList.size - 1)
    }

    // Fungsi untuk mengambil data akhir (Gabungkan jadi List lagi)
    fun getMisiData(): List<String> {
        return misiList
    }

    // Fungsi Update Data Awal dari API
    fun updateData(newList: List<String>) {
        misiList.clear()
        misiList.addAll(newList)
        notifyDataSetChanged()
    }
}