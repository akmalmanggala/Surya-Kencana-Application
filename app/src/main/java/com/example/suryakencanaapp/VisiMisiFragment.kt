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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.MisiAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentVisiMisiBinding
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VisiMisiFragment : Fragment() {

    private var _binding: FragmentVisiMisiBinding? = null
    private val binding get() = _binding!!
    private lateinit var misiAdapter: MisiAdapter
    private var loadingDialog: android.app.AlertDialog? = null

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
        misiAdapter = MisiAdapter(mutableListOf())
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
            binding.btnSaveVisiMisi.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSaveVisiMisi.isEnabled = true
        }
    }

    private fun fetchData() {
        _binding?.swipeRefresh?.isRefreshing = true
        lifecycleScope.launch {
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
                            if (rawMission.contains("\n")) {
                                rawMission.split("\n").map { it.trim() }.toMutableList()
                            } else {
                                mutableListOf(rawMission)
                            }
                        }
                        misiAdapter.updateData(missionList)
                    }
                }
            } catch (e: Exception) {
                Log.e("VISIMISI", "Error: ${e.message}")
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

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val response = ApiClient.instance.updateVisiMisi(
                    "Bearer $token",
                    visi,
                    misiJsonString
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, "Visi Misi Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
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