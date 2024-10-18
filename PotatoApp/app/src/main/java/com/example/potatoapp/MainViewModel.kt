package com.example.potatoapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.potatoapp.data.api.ApiConfig
import com.example.potatoapp.data.api.ClassifyResponse
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
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

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
        if (uri != null) {
            classifyImage(uri)
        }
    }

    private fun isConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun classifyImage(uri: Uri) {
        if (!isConnected()) {
            _errorMessage.value = "Tidak ada koneksi internet."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _classificationResult.value = null
            _errorMessage.value = null
            try {
                // Convert URI to File dan kompresi jika perlu
                val file = Utils.uriToFile(uri, context)
                val compressedFile = Utils.compressImage(file)

                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)

                // Panggil API
                val apiService = ApiConfig.getApiService()
                val response: ClassifyResponse = apiService.classifyImage(body)
                if (response.error) {
                    _errorMessage.value = "API Error: ${response.message}"
                } else {
                    _classificationResult.value = response.message
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Terjadi kesalahan pada server: ${e.message()}"
                Log.e("MainViewModel", "HttpException: ${e.message()}", e)
            } catch (e: IOException) {
                _errorMessage.value = "Kesalahan jaringan: ${e.message}"
                Log.e("MainViewModel", "IOException: ${e.message}", e)
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan: ${e.localizedMessage}"
                Log.e("MainViewModel", "Exception: ${e.localizedMessage}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}