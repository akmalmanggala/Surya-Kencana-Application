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

    private fun fetchData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getVisiMisi()

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    if (listData.isNotEmpty()) {
                        val data = listData[0]

                        binding.etVisi.setText(data.vision)

                        // 2. ISI MISI (PERBAIKAN LOGIKA DI SINI)
                        val rawMission = data.mission ?: ""

                        val missionList = try {
                            // Coba parsing sebagai JSON Array ["A", "B", "C"]
                            val type = object : TypeToken<List<String>>() {}.type
                            Gson().fromJson<List<String>>(rawMission, type).toMutableList()
                        } catch (e: Exception) {
                            // Jika gagal (mungkin format teks biasa dipisah Enter), coba split manual
                            if (rawMission.contains("\n")) {
                                rawMission.split("\n").map { it.trim() }.toMutableList()
                            } else {
                                // Jika tidak ada enter dan bukan JSON, anggap 1 baris string
                                mutableListOf(rawMission)
                            }
                        }

                        // Update Adapter agar muncul banyak baris
                        misiAdapter.updateData(missionList)
                    }
                }
            } catch (e: Exception) {
                Log.e("VISIMISI", "Error: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
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

        // Ambil list misi dari adapter
        val listMisi = misiAdapter.getMisiData()

        // Filter: Hapus baris yang kosong agar tidak nyampah
        val validMisi = listMisi.filter { it.isNotBlank() }

        val misiJsonString = Gson().toJson(validMisi)

        if (visi.isEmpty()) {
            Toast.makeText(context, "Visi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil Token
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        lifecycleScope.launch {
            try {
                binding.btnSaveVisiMisi.isEnabled = false
                binding.btnSaveVisiMisi.text = "Updating..."

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
                binding.btnSaveVisiMisi.isEnabled = true
                binding.btnSaveVisiMisi.text = "Simpan Perubahan"
            }
        }
    }
}