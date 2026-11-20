package com.erfansvsh.isocompressor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.erfansvsh.isocompressor.ui.theme.ISOCompressorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    customSeedColor: Color,
    onCustomSeedColorChange: (Color) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Theme Color Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎨 Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Choice 1: System Default (Dynamic Color)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseDynamicColorChange(true) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = useDynamicColor,
                            onClick = { onUseDynamicColorChange(true) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "System Default", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Choice 2: User Choice (Custom Rainbow Colors)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseDynamicColorChange(false) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !useDynamicColor,
                            onClick = { onUseDynamicColorChange(false) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "User Choice", style = MaterialTheme.typography.bodyLarge)
                    }

                    if (!useDynamicColor) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val colors = listOf(
                                Color(0xFF6650a4), // Purple
                                Color(0xFF2196F3), // Blue
                                Color(0xFF009688), // Teal
                                Color(0xFF4CAF50), // Green
                                Color(0xFFFF9800), // Orange
                                Color(0xFFF44336), // Red
                                Color(0xFFE91E63), // Pink
                                Color(0xFFFFEB3B)  // Yellow
                            )

                            colors.forEach { color ->
                                val isSelected = customSeedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { onCustomSeedColorChange(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (color == Color(0xFFFFEB3B)) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Dark / Light Appearance Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🌗 Dark / Light",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Choice 1: System Default
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeModeChange(ThemeMode.SYSTEM) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "System Default", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Choice 2: User Choice (Light / Dark circles)
                    val isUserChoice = themeMode == ThemeMode.LIGHT || themeMode == ThemeMode.DARK
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isUserChoice) onThemeModeChange(ThemeMode.LIGHT)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isUserChoice,
                            onClick = {
                                if (!isUserChoice) onThemeModeChange(ThemeMode.LIGHT)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "User Choice", style = MaterialTheme.typography.bodyLarge)
                    }

                    if (isUserChoice) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            val appearanceOptions = listOf(
                                ThemeMode.DARK to Color(0xFF121212),
                                ThemeMode.LIGHT to Color.White
                            )

                            appearanceOptions.forEach { (mode, bgColor) ->
                                val isSelected = themeMode == mode
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) { onThemeModeChange(mode) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (bgColor == Color.White) Color.Black else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (mode == ThemeMode.LIGHT) "Light" else "Dark",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: IsoCompressorViewModel,
    onBack: () -> Unit
) {
    SettingsScreen(
        useDynamicColor = viewModel.useDynamicColor,
        onUseDynamicColorChange = { viewModel.useDynamicColor = it },
        customSeedColor = viewModel.customSeedColor,
        onCustomSeedColorChange = { viewModel.customSeedColor = it },
        themeMode = viewModel.themeMode,
        onThemeModeChange = { viewModel.themeMode = it },
        onBack = onBack
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ISOCompressorTheme {
        SettingsScreen(
            useDynamicColor = false,
            onUseDynamicColorChange = {},
            customSeedColor = Color(0xFF2196F3),
            onCustomSeedColorChange = {},
            themeMode = ThemeMode.LIGHT,
            onThemeModeChange = {},
            onBack = {}
        )
    }
}
