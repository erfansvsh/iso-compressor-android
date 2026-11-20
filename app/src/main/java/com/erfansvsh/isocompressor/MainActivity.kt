package com.erfansvsh.isocompressor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erfansvsh.isocompressor.ui.theme.ISOCompressorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: IsoCompressorViewModel = viewModel()
            val darkTheme = when (viewModel.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ISOCompressorTheme(
                darkTheme = darkTheme,
                dynamicColor = viewModel.useDynamicColor,
                customSeedColor = viewModel.customSeedColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewModel.currentScreen) {
                        AppScreen.MAIN -> MainScreen(viewModel)
                        AppScreen.SETTINGS -> SettingsScreen(viewModel) { viewModel.currentScreen = AppScreen.MAIN }
                        AppScreen.ABOUT -> AboutScreen { viewModel.currentScreen = AppScreen.MAIN }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedFileName: String? = null,
    fileType: FileType = FileType.UNSUPPORTED,
    errorMessage: String? = null,
    successMessage: String? = null,
    compressionLevel: Int = 9,
    onCompressionLevelChange: (Int) -> Unit = {},
    alignment: Int = 0,
    onAlignmentChange: (Int) -> Unit = {},
    csoHeader: CsoEngine.CsoHeader? = null,
    isProcessing: Boolean = false,
    progress: Float = 0f,
    progressStatusText: String = "",
    onSelectFile: () -> Unit = {},
    onStartCompress: () -> Unit = {},
    onStartDecompress: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateAbout: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ISO 🔄️ CSO") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Appearance") },
                            onClick = {
                                menuExpanded = false
                                onNavigateSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                menuExpanded = false
                                onNavigateAbout()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Input File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedFileName != null) {
                        Text(text = "Selected: $selectedFileName")
                        Text(
                            text = "Type: ${fileType.name}",
                            color = when (fileType) {
                                FileType.ISO -> MaterialTheme.colorScheme.primary
                                FileType.CSO -> MaterialTheme.colorScheme.secondary
                                FileType.UNSUPPORTED -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(text = "No file selected.")
                    }

                    Button(
                        onClick = onSelectFile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select ISO or CSO File")
                    }
                }
            }

            // Error / Success Banners
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (fileType == FileType.UNSUPPORTED) {
                            Text(
                                text = "Supported file formats:\n• Uncompressed: .iso\n• Compressed: .cso",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            successMessage?.let { success ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Success", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = success, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ISO File Workflow: Compression options
            if (fileType == FileType.ISO && !isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Compress ISO to CSO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(text = "Target Format: .cso")

                        Text(text = "Compression Level (1-9): $compressionLevel")
                        Slider(
                            value = compressionLevel.toFloat(),
                            onValueChange = { onCompressionLevelChange(it.toInt()) },
                            valueRange = 1f..9f,
                            steps = 7
                        )

                        Text(text = "Alignment (0-6): $alignment")
                        Slider(
                            value = alignment.toFloat(),
                            onValueChange = { onAlignmentChange(it.toInt()) },
                            valueRange = 0f..6f,
                            steps = 5
                        )
                        Text(
                            text = "Note: 0 = small/slow, 6 = fast/large",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onStartCompress,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Compression")
                        }
                    }
                }
            }

            // CSO File Workflow: Decompression prompt
            if (fileType == FileType.CSO && !isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Decompress CSO to ISO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        csoHeader?.let { header ->
                            Text(text = "ISO Size: ${header.isoSize / (1024 * 1024)} MB")
                            Text(text = "Block Size: ${header.blockSize} bytes")
                            Text(text = "Alignment: ${header.align}")
                            Text(text = "Version: ${header.version}")
                        }

                        Text(text = "Would you like to decompress this compressed file back to ISO?")

                        Button(
                            onClick = onStartDecompress,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Decompression")
                        }
                    }
                }
            }

            // Processing Progress Indicator
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = progressStatusText, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(text = "${(progress * 100).toInt()}%")
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: IsoCompressorViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            if (pendingAction == "compress") {
                viewModel.startCompress(context, uri)
            } else if (pendingAction == "decompress") {
                viewModel.startDecompress(context, uri)
            }
        }
        pendingAction = null
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            if (name == null) {
                name = uri.lastPathSegment ?: "unknown_file"
            }
            viewModel.onFileSelected(context, uri, name)
        }
    }

    MainScreen(
        selectedFileName = viewModel.selectedFileName,
        fileType = viewModel.fileType,
        errorMessage = viewModel.errorMessage,
        successMessage = viewModel.successMessage,
        compressionLevel = viewModel.compressionLevel,
        onCompressionLevelChange = { viewModel.compressionLevel = it },
        alignment = viewModel.alignment,
        onAlignmentChange = { viewModel.alignment = it },
        csoHeader = viewModel.csoHeader,
        isProcessing = viewModel.isProcessing,
        progress = viewModel.progress,
        progressStatusText = viewModel.progressStatusText,
        onSelectFile = { openFileLauncher.launch(arrayOf("*/*")) },
        onStartCompress = {
            pendingAction = "compress"
            val defaultOutputName = (viewModel.selectedFileName?.substringBeforeLast('.') ?: "compressed") + ".cso"
            saveFileLauncher.launch(defaultOutputName)
        },
        onStartDecompress = {
            pendingAction = "decompress"
            val defaultOutputName = (viewModel.selectedFileName?.substringBeforeLast('.') ?: "decompressed") + ".iso"
            saveFileLauncher.launch(defaultOutputName)
        },
        onNavigateSettings = { viewModel.currentScreen = AppScreen.SETTINGS },
        onNavigateAbout = { viewModel.currentScreen = AppScreen.ABOUT }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ISOCompressorTheme {
        MainScreen(
            selectedFileName = "sample_game.iso",
            fileType = FileType.ISO,
            compressionLevel = 9,
            alignment = 0
        )
    }
}

