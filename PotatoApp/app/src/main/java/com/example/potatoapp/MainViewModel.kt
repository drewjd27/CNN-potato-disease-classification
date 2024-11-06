// MainViewModel.kt
package com.example.potatoapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.potatoapp.ml.ModelE // Pastikan ini sesuai dengan nama kelas model Anda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class MainViewModel : ViewModel() {

    private val context: Context = try {
        PotatoApp.getAppContext()
    } catch (e: IllegalStateException) {
        Log.e("MainViewModel", "Context tidak tersedia: ${e.message}")
        throw e
    }

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _classificationResult = MutableLiveData<String?>()
    val classificationResult: LiveData<String?> get() = _classificationResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    var imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
        if (uri != null) {
            classifyImage(uri)
        }
    }

    private fun classifyImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _classificationResult.postValue(null)
            _errorMessage.postValue(null)
            try {
                var tensorImage = TensorImage(DataType.FLOAT32)
                // Konversi URI ke Bitmap
                val bitmap = Utils.uriToBitmap(uri, context)
                tensorImage.load(bitmap)

                // Resize bitmap sesuai dengan input model
                tensorImage = imageProcessor.process(tensorImage)

                // Priproses gambar untuk model

                // Muat model TensorFlow Lite
                val model = ModelE.newInstance(context)

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
                inputFeature0.loadBuffer(tensorImage.buffer)

                // Jalankan inferensi
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                // Dapatkan hasil klasifikasi
                val probabilities = outputFeature0.floatArray
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
                val classNames = listOf("Busuk Awal", "Sehat", "Busuk")
                val detectedClass = if (maxIndex in classNames.indices) classNames[maxIndex] else "Tidak Diketahui"

                // Tutup model
                model.close()

                // Update LiveData dengan hasil klasifikasi
                _classificationResult.postValue(detectedClass)
            } catch (e: IOException) {
                _errorMessage.postValue("Gagal memuat model: ${e.message}")
                Log.e("MainViewModel", "IOException: ${e.message}", e)
            } catch (e: Exception) {
                _errorMessage.postValue("Terjadi kesalahan: ${e.localizedMessage}")
                Log.e("MainViewModel", "Exception: ${e.localizedMessage}", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
