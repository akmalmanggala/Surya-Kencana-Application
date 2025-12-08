package com.example.suryakencanaapp.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.databinding.ItemMisiBinding

class MisiAdapter(
    private var misiList: MutableList<String>
) : RecyclerView.Adapter<MisiAdapter.ViewHolder>() {

    // Variabel untuk mencegah double click
    private var lastClickTime: Long = 0

    class ViewHolder(val binding: ItemMisiBinding) : RecyclerView.ViewHolder(binding.root) {
        // Simpan referensi TextWatcher agar bisa dihapus nanti
        var currentWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMisiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 1. HAPUS TextWatcher lama sebelum mengisi teks baru (PENTING!)
        // Jika tidak dihapus, saat scroll listener akan menumpuk dan bikin error/lemot.
        holder.currentWatcher?.let {
            holder.binding.etMisiItem.removeTextChangedListener(it)
        }

        // 2. Isi teks dari list
        holder.binding.etMisiItem.setText(misiList[position])

        // 3. Buat TextWatcher baru
        val newWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Update data di list hanya jika posisi valid
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    misiList[holder.adapterPosition] = s.toString()
                }
            }
        }

        // Pasang listener baru dan simpan referensinya
        holder.binding.etMisiItem.addTextChangedListener(newWatcher)
        holder.currentWatcher = newWatcher

        // 4. Tombol Hapus (DENGAN ANTI-DOUBLE CLICK)
        holder.binding.btnDeleteMisi.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            // Cek 1: Jeda waktu (500ms) agar tidak bisa diklik 2x cepat
            if (currentTime - lastClickTime > 500) {
                lastClickTime = currentTime

                val currentPos = holder.adapterPosition

                // Cek 2: Pastikan posisi valid (tidak sedang animasi hapus)
                if (currentPos != RecyclerView.NO_POSITION && currentPos < misiList.size) {
                    misiList.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                    // Update range agar index di bawahnya bergeser dengan benar
                    notifyItemRangeChanged(currentPos, misiList.size)
                }
            }
        }
    }

    override fun getItemCount() = misiList.size

    fun addEmptyItem() {
        misiList.add("")
        notifyItemInserted(misiList.size - 1)
    }

    fun getMisiData(): List<String> {
        return misiList
    }

    fun updateData(newList: List<String>) {
        misiList.clear()
        misiList.addAll(newList)
        notifyDataSetChanged()
    }
}