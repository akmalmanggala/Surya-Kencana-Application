package com.example.suryakencanaapp

import android.app.AlertDialog // Pastikan import ini
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.MisiAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentVisiMisiBinding
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException

class VisiMisiFragment : Fragment() {

    private var _binding: FragmentVisiMisiBinding? = null
    private val binding get() = _binding!!
    private lateinit var misiAdapter: MisiAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisiMisiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMisi.layoutManager = LinearLayoutManager(context)

        // 1. Inisialisasi Adapter dengan Callback Delete
        misiAdapter = MisiAdapter(mutableListOf()) { position ->
            showDeleteConfirmation(position)
        }
        binding.rvMisi.adapter = misiAdapter

        binding.btnAddMisiPoint.setOnClickListener {
            misiAdapter.addEmptyItem()
        }

        binding.btnSaveVisiMisi.setOnClickListener {
            saveData()
        }
        binding.swipeRefresh.setOnRefreshListener {
            fetchData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 2. Fungsi Menampilkan Dialog Konfirmasi Hapus Misi
    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Poin Misi")
            .setMessage("Apakah Anda yakin ingin menghapus poin misi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                // Jika Ya, panggil fungsi hapus di adapter
                misiAdapter.removeItem(position)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- HELPER LOADING ---
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = AlertDialog.Builder(requireContext())
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false)
                loadingDialog = builder.create()
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
            binding.btnSaveVisiMisi.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSaveVisiMisi.isEnabled = true
        }
    }

    private fun fetchData() {
        _binding?.swipeRefresh?.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getVisiMisi()
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!
                    if (listData.isNotEmpty()) {
                        val data = listData[0]
                        binding.etVisi.setText(data.vision)
                        val rawMission = data.mission ?: ""
                        val missionList = try {
                            val type = object : TypeToken<List<String>>() {}.type
                            Gson().fromJson<List<String>>(rawMission, type).toMutableList()
                        } catch (e: Exception) {
                            if (rawMission.contains("\n")) rawMission.split("\n").map { it.trim() }.toMutableList() else mutableListOf(rawMission)
                        }
                        misiAdapter.updateData(missionList)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Ignore
                } else {
                    Log.e("VISIMISI", "Error: ${e.message}")
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.post {
            fetchData()
        }
    }

    private fun saveData() {
        val visi = binding.etVisi.text.toString().trim()
        val listMisi = misiAdapter.getMisiData()
        val validMisi = listMisi.filter { it.isNotBlank() }
        val misiJsonString = Gson().toJson(validMisi)

        if (visi.isEmpty()) {
            Toast.makeText(context, "Visi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setLoading(true)
                val response = ApiClient.instance.updateVisiMisi("Bearer $token", visi, misiJsonString)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Visi Misi Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Ignore
                } else {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                setLoading(false)
            }
        }
    }
}