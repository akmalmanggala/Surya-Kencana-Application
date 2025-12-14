package com.example.suryakencanaapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.suryakencanaapp.EditProdukActivity
import com.example.suryakencanaapp.databinding.ItemProdukBinding
import com.example.suryakencanaapp.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProdukAdapter(
    private var productList: MutableList<Product>,
    private val onDeleteClick: (Product) -> Unit,
    private val onEditClick: (Product) -> Unit // Callback ini bisa dihapus jika tidak dipakai
) : RecyclerView.Adapter<ProdukAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ItemProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        with(holder.binding) {
            tvProductName.text = product.name
            tvProductDesc.text = product.description ?: "Tidak ada deskripsi"

            val priceDouble = product.price.toDoubleOrNull() ?: 0.0
            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            tvProductPrice.text = formatRupiah.format(priceDouble)

            if (product.hidePrice == 1) {
                tvHidePriceBadge.visibility = View.VISIBLE
            } else {
                tvHidePriceBadge.visibility = View.GONE
            }

            if (!product.imageUrl.isNullOrEmpty()) {
                tvNoImage.visibility = View.GONE
                Glide.with(root.context)
                    .load(product.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imgProduct)
            } else {
                tvNoImage.visibility = View.VISIBLE
                imgProduct.setImageDrawable(null)
            }

            // --- LOGIKA EDIT YANG DIPERBAIKI ---
            val performEdit = {
                val intent = Intent(root.context, EditProdukActivity::class.java)
                intent.putExtra("ID", product.id)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("HIDE_PRICE", product.hidePrice)
                intent.putExtra("IMAGE_URL", product.imageUrl)
                root.context.startActivity(intent)
            }

            // 1. Klik Tombol Edit -> Edit
            btnEdit.setOnClickListener { performEdit() }

            // 2. Klik Kartu -> Edit
            root.setOnClickListener { performEdit() }

            // Klik Tombol Hapus
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
    }

    override fun getItemCount() = productList.size

    fun updateData(newProducts: List<Product>) {
        productList.clear()
        productList.addAll(newProducts)
        notifyDataSetChanged()
    }
}