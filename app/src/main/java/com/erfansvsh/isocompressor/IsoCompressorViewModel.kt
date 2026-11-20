package com.erfansvsh.isocompressor

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class FileType {
    UNSUPPORTED, ISO, CSO
}

enum class AppScreen {
    MAIN, SETTINGS, ABOUT
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class IsoCompressorViewModel(application: Application) : AndroidViewModel(application) {
    // Navigation screen state
    var currentScreen by mutableStateOf(AppScreen.MAIN)

    // Theme settings state
    var useDynamicColor by mutableStateOf(true)
    var customSeedColor by mutableStateOf(Color(0xFF2196F3)) // Bluish default
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set
    var selectedFileName by mutableStateOf<String?>(null)
        private set
    var fileType by mutableStateOf(FileType.UNSUPPORTED)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    // Compression options
    var compressionLevel by mutableStateOf(9)
    var alignment by mutableStateOf(0)

    // CSO info
    var csoHeader by mutableStateOf<CsoEngine.CsoHeader?>(null)
        private set

    // Progress
    var isProcessing by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set
    var progressStatusText by mutableStateOf("")
        private set

    fun onFileSelected(context: android.content.Context, uri: Uri?, name: String?) {
        selectedFileUri = uri
        selectedFileName = name
        errorMessage = null
        successMessage = null
        csoHeader = null

        if (uri == null || name == null) {
            fileType = FileType.UNSUPPORTED
            return
        }

        val lowerName = name.lowercase()
        when {
            lowerName.endsWith(".iso") -> {
                fileType = FileType.ISO
            }
            lowerName.endsWith(".cso") -> {
                fileType = FileType.CSO
                val header = CsoEngine.readCsoHeader(context, uri)
                if (header != null) {
                    csoHeader = header
                } else {
                    errorMessage = "Invalid CSO file format or corrupted header."
                    fileType = FileType.UNSUPPORTED
                }
            }
            else -> {
                fileType = FileType.UNSUPPORTED
                errorMessage = "Unsupported file format! Supported formats: .iso (uncompressed) and .cso (compressed)."
            }
        }
    }

    fun startCompress(context: android.content.Context, outputUri: Uri) {
        val input = selectedFileUri ?: return
        isProcessing = true
        progress = 0f
        progressStatusText = "Starting compression..."
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                CsoEngine.compressIso(
                    context = context,
                    inputUri = input,
                    outputUri = outputUri,
                    level = compressionLevel,
                    align = alignment,
                ) { prog, status ->
                    progress = prog
                    progressStatusText = status
                }
                successMessage = "Compression completed successfully!"
            } catch (e: Exception) {
                errorMessage = "Compression failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun startDecompress(context: android.content.Context, outputUri: Uri) {
        val input = selectedFileUri ?: return
        isProcessing = true
        progress = 0f
        progressStatusText = "Starting decompression..."
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                CsoEngine.decompressCso(
                    context = context,
                    inputUri = input,
                    outputUri = outputUri,
                ) { prog, status ->
                    progress = prog
                    progressStatusText = status
                }
                successMessage = "Decompression completed successfully!"
            } catch (e: Exception) {
                errorMessage = "Decompression failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }
}
