package com.example.suryakencanaapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = "upload_temp_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, fileName)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // PROSES KOMPRESI + RESIZE
            val compressedFile = compressImage(tempFile)

            Log.d("FileUtils", "Size Akhir: ${compressedFile.length() / 1024} KB")

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImage(file: File): File {
        try {
            // 1. Cek Ukuran Asli Dulu (Tanpa memuat gambar ke memori)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            // 2. Hitung Skala (Agar max resolusi cuma 1280px, misal)
            // Ini yang bikin HP kentang pun jadi ngebut!
            options.inSampleSize = calculateInSampleSize(options, 1280, 1280)

            // 3. Decode Ulang dengan Ukuran Kecil
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(file.path, options)

            // 4. Kompres & Timpa File
            val outStream = ByteArrayOutputStream()
            // Kualitas 70% sudah sangat bagus jika resolusi sudah dikecilkan
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outStream)

            val fileOut = FileOutputStream(file)
            fileOut.write(outStream.toByteArray())
            fileOut.flush()
            fileOut.close()

            return file
        } catch (e: Exception) {
            Log.e("FileUtils", "Gagal kompres: ${e.message}")
            return file
        }
    }

    // Rumus Matematika untuk mengecilkan gambar
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}