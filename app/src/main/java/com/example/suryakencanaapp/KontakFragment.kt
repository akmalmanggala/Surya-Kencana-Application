package com.example.suryakencanaapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentKontakBinding
import kotlinx.coroutines.launch

class KontakFragment : Fragment() {

    private var _binding: FragmentKontakBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: android.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKontakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            fetchContactData()
        }

        binding.btnSaveContact.setOnClickListener {
            saveContactData()
        }

        fetchContactData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.post {
            fetchContactData()
        }
    }

    // --- HELPER LOADING ---
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = android.app.AlertDialog.Builder(requireContext())
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false) // User tidak bisa back
                loadingDialog = builder.create()
                // Agar background transparan (hanya card yang terlihat)
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
            binding.btnSaveContact.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSaveContact.isEnabled = true
        }
    }

    private fun fetchContactData() {
        _binding?.swipeRefresh?.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getContact()

                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    if (listData.isNotEmpty()) {
                        val data = listData[0]
                        binding.etEmail.setText(data.email)
                        binding.etWhatsapp.setText(data.phone)
                        binding.etAddress.setText(data.address)
                        binding.etMaps.setText(data.mapUrl)
                    }
                } else {
                    if (_binding != null)
                        Toast.makeText(context, "Gagal memuat: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CONTACT_API", "Error: ${e.message}")
                if (_binding != null)
                    Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun saveContactData() {
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etWhatsapp.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val maps = binding.etMaps.text.toString().trim()

        if (email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(context, "Email, WA, dan Alamat wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val response = ApiClient.instance.updateContact(
                    "Bearer $token",
                    email,
                    phone,
                    address,
                    maps
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, "Kontak Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("CONTACT_UPDATE", "Error: $errorMsg")
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // 2. Sembunyikan Overlay
                setLoading(false)
            }
        }
    }
}