package com.example.suryakencanaapp.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.databinding.ItemMisiBinding

class MisiAdapter(
    private var misiList: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit // 1. Callback ke Fragment
) : RecyclerView.Adapter<MisiAdapter.ViewHolder>() {

    // Variabel untuk mencegah double click
    private var lastClickTime: Long = 0

    class ViewHolder(val binding: ItemMisiBinding) : RecyclerView.ViewHolder(binding.root) {
        var currentWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMisiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.currentWatcher?.let {
            holder.binding.etMisiItem.removeTextChangedListener(it)
        }

        holder.binding.etMisiItem.setText(misiList[position])

        val newWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    misiList[holder.adapterPosition] = s.toString()
                }
            }
        }

        holder.binding.etMisiItem.addTextChangedListener(newWatcher)
        holder.currentWatcher = newWatcher

        // 2. Tombol Hapus: Panggil Callback, jangan langsung hapus
        holder.binding.btnDeleteMisi.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 500) {
                lastClickTime = currentTime
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    // Panggil fungsi di Fragment (show dialog)
                    onDeleteClick(currentPos)
                }
            }
        }
    }

    override fun getItemCount() = misiList.size

    // 3. Fungsi Publik untuk menghapus item (Dipanggil oleh Fragment setelah konfirmasi)
    fun removeItem(position: Int) {
        if (position >= 0 && position < misiList.size) {
            misiList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, misiList.size)
        }
    }

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