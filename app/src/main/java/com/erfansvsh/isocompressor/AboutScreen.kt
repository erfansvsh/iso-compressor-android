package com.erfansvsh.isocompressor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.erfansvsh.isocompressor.ui.theme.ISOCompressorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val imageLoader = remember {
        if (isPreview) null else ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "ISO ⇆ CSO Converter",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "Developed by Erfan Siavoshy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val annotatedString = buildAnnotatedString {
                        append("It's a personal project at GitHub so you can review its source code at the link below for security concerns or for educational purposes, or for forking your very own build based on it:\n\n")
                        pushStringAnnotation(tag = "URL", annotation = "https://github.com/erfansvsh/iso-compressor-android")
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF2196F3),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("github.com/erfansvsh/iso-compressor-android/")
                        }
                        pop()
                    }

                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )

                    HorizontalDivider()

                    Text(
                        text = "⚙️ The Engine (csochef.py)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The core compression and decompression logic is a faithful, optimized native Kotlin port of RetroChef's csochef Python script (github.com/RetroChef/csochef).\n\nInstead of bundling a heavy Python runtime, the entire algorithm—including block-by-block raw DEFLATE streaming, offset tables, header parsing, and alignment padding—was translated into pure, high-performance Kotlin coroutines running on Android's Storage Access Framework (SAF).",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    Text(
                        text = "🛠️ Built With",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 3 Equal-Width Columns with Modifier.weight(1f) and exact custom icon sizes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. GitHub (32.dp)
                        Box(modifier = Modifier.weight(1f)) {
                            TechCardAsync(
                                model = "file:///android_asset/GitHub_Invertocat_Logo.svg",
                                imageLoader = imageLoader,
                                title = "GitHub",
                                subtitle = "csochef engine",
                                imageSize = 32.dp
                            )
                        }
                        // 2. Kotlin (94.35% of 32dp = ~30.2dp)
                        Box(modifier = Modifier.weight(1f)) {
                            TechCardAsync(
                                model = "file:///android_asset/Kotlin_cube.svg",
                                imageLoader = imageLoader,
                                title = "Kotlin",
                                subtitle = "Native IO",
                                imageSize = 30.2.dp
                            )
                        }
                        // 3. Jetpack Compose (131.67% of 32dp = ~42.1dp)
                        Box(modifier = Modifier.weight(1f)) {
                            TechCardAsync(
                                model = "file:///android_asset/Jetpack_Compose_logo.svg",
                                imageLoader = imageLoader,
                                title = "Jetpack",
                                subtitle = "Compose M3",
                                imageSize = 42.1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechCardAsync(model: Any, imageLoader: ImageLoader?, title: String, subtitle: String, imageSize: Dp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageLoader != null) {
                    AsyncImage(
                        model = model,
                        imageLoader = imageLoader,
                        contentDescription = title,
                        modifier = Modifier.size(imageSize)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontSize = 9.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    ISOCompressorTheme {
        AboutScreen(onBack = {})
    }
}
