# 💿 ISO ⇆ CSO Compressor — Native CSO Compression Engine for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2F%20Unidirectional-00C853?style=flat-square)](#-system-architecture--unidirectional-data-flow)
[![Engine](https://img.shields.io/badge/Core%20Engine-Native%20Kotlin%20DEFLATE-FF6D00?style=flat-square)](#-core-engine-binary-layout--deflate-streaming)
[![Storage](https://img.shields.io/badge/Storage-Android%20SAF-009688?style=flat-square)](#-storage-access-framework-saf--io-pipeline)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

An architectural and native Kotlin engineering implementation of an **ISO ⇆ CSO Disk Image Compressor & Decompressor** for Android, written completely from scratch in 100% modern **Kotlin** using **Jetpack Compose** and **Material 3**.

Instead of bundling a bloated, slow Python interpreter on mobile, this repository faithfully translates RetroChef’s [`csochef`](https://github.com/RetroChef/csochef) algorithm into **pure, high-performance Kotlin coroutines** running directly over `java.util.zip.Deflater` / `Inflater` raw DEFLATE streams. It delivers **2048-byte sector-level compression**, **configurable DEFLATE levels (1–9)**, **bit-shifted alignment padding (0–6)**, **raw-block fallback preservation**, **CSO header inspection**, **Android Storage Access Framework (SAF) integration**, and **Material You dynamic theming**.

---

## 📸 Visual Showcase & Functional Workflows

| ISO → CSO Compression Workflow | CSO → ISO Decompression & Inspector |
| :---: | :---: |
| Select any uncompressed `.iso` image via Android’s native document picker, fine-tune **Compression Level (1–9)** and **Alignment (0–6)** with interactive Material 3 sliders, and monitor real-time block-by-block progress. | Automatically validates the 24-byte `CISO` magic header (`0x4F534943`), displays original ISO size (MB), block size (`2048 B`), alignment exponent, and format version before restoring bit-exact `.iso` output. |

---

## 💡 Non-Coder Conceptual Introduction

> **How does an ISO compression app actually work?**

Think of this application as a high-speed, automated freight-packing warehouse:

1. **The Front Desk (The UI / Compose Screens):** `MainScreen`, `SettingsScreen`, and `AboutScreen` form the visual interface. You hand over an `.iso` or `.cso` file ticket and adjust your packing sliders. The UI *never touches raw bytes*—it simply sends your instructions to the supervisor and displays the progress bar.
2. **The Floor Supervisor (`IsoCompressorViewModel`):** The ViewModel manages all application state—selected file metadata, error/success banners, compression sliders, and active theme settings. Even if you rotate your phone mid-conversion, the supervisor keeps the operation running smoothly without losing track.
3. **The Packing Line (`CsoEngine` on `Dispatchers.IO`):** Running on a dedicated background thread so the screen never freezes, `CsoEngine` slices a giant disk image into thousands of uniform **2048-byte boxes (sectors)**. Each box is vacuum-sealed (`Deflater`). If a box refuses to shrink, it is flagged and packed as-is so zero space is wasted, then indexed in a master lookup table at the front of the `.cso` file.

---

## 🏗️ System Architecture & Unidirectional Data Flow

The application follows clean **MVVM + Unidirectional Data Flow (UDF)** principles. Compose screens observe `mutableStateOf` state properties exposed by `IsoCompressorViewModel` and dispatch user intents back up, while all binary file I/O and raw DEFLATE math execute asynchronously on `Dispatchers.IO`.

```
┌────────────────────────────────────────────────────────┐
│              MainActivity & Compose UI Layer           │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │     MainScreen     │      │   SettingsScreen   │   │
│   │  (SAF & Sliders)   │      │  (Material You/M3) │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ User Actions & URIs      │ Compose State Updates
              ▼                          │
┌────────────────────────────────────────┴───────────────┐
│                IsoCompressorViewModel                  │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │ File Validation &  │      │   viewModelScope   │   │
│   │ Header Inspection  │      │ Progress Callbacks │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ Suspend Execution        │ Progress (0.0..1.0)
              ▼                          │
┌────────────────────────────────────────┴───────────────┐
│               CsoEngine (Dispatchers.IO)               │
│                                                        │
│   • 24-Byte CISO Header I/O     • 2048-Byte Sectoring  │
│   • Raw DEFLATE (nowrap=true)   • Bit-Shifted Align    │
│   • 0x80000000 Raw Fallback     • Cache Auto-Cleanup   │
└────────────────────────────────────────────────────────┘
```

---

## 🧠 ViewModel State Management & Navigation Architecture

The application's reactive state and coroutine lifecycle are encapsulated within `IsoCompressorViewModel.kt`. It isolates UI presentation from file-system operations while preserving state across configuration changes.

### Key State Properties & Enums

```kotlin
enum class FileType { UNSUPPORTED, ISO, CSO }
enum class AppScreen { MAIN, SETTINGS, ABOUT }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class IsoCompressorViewModel(application: Application) : AndroidViewModel(application) {
    // Navigation & Material 3 Appearance State
    var currentScreen by mutableStateOf(AppScreen.MAIN)
    var useDynamicColor by mutableStateOf(true)
    var customSeedColor by mutableStateOf(Color(0xFF2196F3))
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    // Selected File & Parsed CSO Metadata
    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set
    var fileType by mutableStateOf(FileType.UNSUPPORTED)
        private set
    var csoHeader by mutableStateOf<CsoEngine.CsoHeader?>(null)
        private set

    // Tunable Compression Parameters
    var compressionLevel by mutableStateOf(9) // Range: 1 (fastest) .. 9 (smallest)
    var alignment by mutableStateOf(0)        // Range: 0 (1-byte)  .. 6 (64-byte)

    // Asynchronous Conversion Progress
    var isProcessing by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set
    var progressStatusText by mutableStateOf("")
        private set
}
```

---

## ⚙️ Core Engine, Binary Layout & DEFLATE Streaming

### CSO Binary Specification (`CISO` v1)

`CsoEngine` reads and writes standard little-endian **CISO** disk images compatible with PSP/PS2 emulators and homebrew loaders:

| Offset | Size | Field | Description |
| :--- | :--- | :--- | :--- |
| `0x00` | 4 Bytes | `magic` | Little-endian ASCII `"CISO"` (`0x4F534943`) |
| `0x04` | 4 Bytes | `headerSize` | Fixed `24` bytes (`0x18`) |
| `0x08` | 8 Bytes | `isoSize` | Uncompressed original ISO size in bytes (`UInt64LE`) |
| `0x10` | 4 Bytes | `blockSize` | Sector size fixed at `2048` bytes (`0x800`) |
| `0x14` | 1 Byte | `version` | CSO format version (`1`) |
| `0x15` | 1 Byte | `align` | Alignment bit-shift exponent ($2^{\text{align}}$ byte boundary) |
| `0x16` | 2 Bytes | `reserved` | Zero-padded alignment bytes (`0x0000`) |
| `0x18` | $(N+1) \times 4$ B | `offsets[]` | Little-endian sector index table (MSB `0x80000000` flags raw blocks) |

### Raw DEFLATE Streaming & Incompressible Block Fallback

Each 2048-byte ISO sector is compressed using `Deflater(level, true)` (`nowrap = true` to omit zlib headers/checksums per the CSO spec). If compressing an already-compressed or random sector produces output $\ge 2048$ bytes, `CsoEngine` stores the raw sector instead and sets bit `31` (`0x80000000`) on its offset entry:

```kotlin
val pad = (currentOffset and alignMask)
if (pad != 0) {
    val padding = alignBytes - pad
    cso.write(ByteArray(padding))
    currentOffset += padding
}

if (compressed.size < actualData.size) {
    offsets[block] = currentOffset ushr align
    cso.write(compressed)
    currentOffset += compressed.size
} else {
    // Flag MSB (0x80000000) to indicate an uncompressed raw sector
    offsets[block] = ((currentOffset ushr align) or 0x80000000.toInt())
    cso.write(actualData)
    currentOffset += actualData.size
}
```

### Alignment vs. Compression Trade-off Matrix

The `alignment` parameter shifts sector offsets by $2^{\text{align}}$ bytes, allowing hardware or emulators to seek faster at the cost of minor zero-padding overhead:

| Alignment Value | Byte Boundary ($2^n$) | Best Use Case |
| :---: | :---: | :--- |
| **`0`** *(Default)* | $1\text{ byte}$ | Maximum compression ratio; zero padding waste |
| **`1` – `2`** | $2\text{ – }4\text{ bytes}$ | Balanced read alignment for modern flash storage |
| **`3` – `6`** | $8\text{ – }64\text{ bytes}$ | Fast block-aligned seeking on legacy/strict I/O loaders |

### Storage Access Framework (SAF) & I/O Pipeline

Because Android’s `ContentResolver` URIs do not natively expose random-access seek pointers required to backfill the CSO offset table after compression completes:
1. Input streams from `ActivityResultContracts.OpenDocument()` are buffered into `context.cacheDir`.
2. `RandomAccessFile` performs high-speed binary seeks to write sector data and patch the header + index table in place.
3. The finalized file is streamed out to the user-selected `ActivityResultContracts.CreateDocument()` URI, and a `finally` block guarantees temporary cache files are purged even if an error occurs.

---

## 🎨 Material 3 Theming & Customization Architecture

The UI layer (`ISOCompressorTheme` & `SettingsScreen`) provides full Material 3 personalization:
- **System Dynamic Color (Material You):** Automatically extracts wallpaper tonal palettes on Android 12+ (`Build.VERSION_CODES.S`) via `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
- **8 Custom Seed Color Swatches:** When Dynamic Color is disabled, users can pick from 8 curated Material seed colors (Purple, Blue, Teal, Green, Orange, Red, Pink, Yellow) with automatic WCAG luminance contrast calculation (`isLightColor`) to switch `onPrimary` text between black and white.
- **Tri-State Appearance Mode:** Independent toggle between **System Default**, **Light**, and **Dark** surface themes.
- **Vector SVG Asset Rendering:** `AboutScreen` renders crisp vector technology badges (`GitHub_Invertocat_Logo.svg`, `Kotlin_cube.svg`, `Jetpack_Compose_logo.svg`) using Coil’s `SvgDecoder`.

---

## 🛠️ Key Advantages Over Traditional Mobile Tools

| Feature | ISO ⇆ CSO Compressor | Python-Bundled Wrappers |
| :--- | :--- | :--- |
| **Runtime Footprint** | **Pure Kotlin / JVM** (Zero Python runtime bloat) | 30–60 MB+ embedded Python interpreter |
| **Execution Speed** | Native JNI-backed `java.util.zip.Deflater` / `Inflater` | Interpreted bytecode overhead |
| **Scoped Storage** | Native Android **SAF** (`OpenDocument` / `CreateDocument`) | Legacy storage permission workarounds |
| **UI Responsiveness** | Asynchronous `Dispatchers.IO` coroutines with live progress | Blocking CLI wrappers or frozen UI threads |
| **CSO Diagnostics** | Built-in 24-byte header parser (`isoSize`, `blockSize`, `align`) | Blind conversion without metadata preview |

---

## 🛠️ Build & Installation Guide

### Requirements
- **Android Studio:** Hedgehog | 2023.1.1 or newer
- **Kotlin:** 1.9.0+
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **Java/JDK:** 17

### Local Compilation

```bash
# Clone Repository
git clone https://github.com/erfansvsh/iso-compressor-android.git
cd iso-compressor-android

# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APK
./gradlew :app:assembleRelease
```

---

## 📂 Project Structure

```
com.erfansvsh.isocompressor
 ├── MainActivity.kt               # MainScreen composable, SAF document launchers & progress UI
 ├── IsoCompressorViewModel.kt     # MVVM state holder, file validation & coroutine orchestration
 ├── CsoEngine.kt                  # Native Kotlin CSO compressor, decompressor & header parser
 ├── SettingsScreen.kt             # Material You dynamic color, seed swatches & Light/Dark toggles
 ├── AboutScreen.kt                # Project info, csochef attribution & Coil SVG tech cards
 └── ui/theme/
      ├── Color.kt                 # Base Material 3 color tokens
      ├── Theme.kt                 # Dynamic & luminance-aware custom seed ColorScheme builder
      └── Type.kt                  # Material 3 typography definitions
```

---

## 🙏 Acknowledgments

- **RetroChef (`csochef`):** Core CISO binary structure, block alignment shifting, and raw DEFLATE streaming logic ported from [RetroChef/csochef](https://github.com/RetroChef/csochef).

---

## 📜 License

Developed by **Erfan Siavoshy**. This project is open source and available under the [MIT License](LICENSE).