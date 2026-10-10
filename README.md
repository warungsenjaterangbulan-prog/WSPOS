# Kasir Terang Bulan — Warung Senja (Native Android Kotlin & Jetpack Compose)

Aplikasi Kasir POS (Point of Sale) dan Manajemen Menu untuk **Warung Senja Terang Bulan** (`Jl. Raya Bedadung Wisata Senja`, Telp/WA: `085881028733`, `Buka Jam 15.00 - 22.00 WIB`) yang dibangun menggunakan **Android Native (Kotlin + Jetpack Compose Material 3)** dengan pola **Clean Architecture + MVVM**.

---

## Arsitektur & Teknologi Modern

Proyek ini dirancang agar **bebas error dependensi Gradle** ketika dibuka langsung di **Android Studio (Ladybug / Koala / Iguana / Meerkat)**:
- **Bahasa**: Kotlin `2.0.21`
- **UI Toolkit**: Jetpack Compose (BOM `2024.10.00`) + Material 3 + Material Icons Extended
- **Android Gradle Plugin (AGP)**: `8.7.2` + Gradle Wrapper `8.10.2`
- **JDK**: Java 17 (dilengkapi `gradle-daemon-jvm.properties` agar otomatis memakai JDK 17)
- **Arsitektur**: Clean Architecture (`domain`, `data`, `presentation`, `di`) + MVVM (`StateFlow` & `ViewModel`)
- **Zero Fragile Third-Party Dependencies**:
  - Penyimpanan lokal menggunakan `SharedPreferences` + `org.json` bawaan Android SDK.
  - Sinkronisasi Cloud Google Sheets menggunakan `HttpURLConnection` + Kotlin Coroutines (`Dispatchers.IO`).
  - Cetak Struk menggunakan **Native Android Bluetooth Socket (`BluetoothAdapter` / RFCOMM SPP `00001101-0000-1000-8000-00805F9B34FB`)** dengan generator **ESC/POS Byte Command & GS v 0 Raster Bitmap Logo** serta fallback **Android System `PrintManager` (WebView Print Adapter)**.

---

## Struktur Folder Lengkap (Clean Architecture)

```text
├── build.gradle.kts                  # Konfigurasi Gradle level root
├── settings.gradle.kts               # Definisi rootProject & modul :app
├── gradle.properties                 # Konfigurasi JVM & AndroidX
├── gradlew & gradlew.bat             # Script eksekusi Gradle Wrapper (Linux/macOS/Windows)
├── gradle/
│   ├── libs.versions.toml            # Version Catalog terpusat (AGP, Kotlin, Compose BOM)
│   ├── gradle-daemon-jvm.properties  # Auto-provisioning JDK 17
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts              # Konfigurasi modul aplikasi Android & Jetpack Compose
    ├── proguard-rules.pro            # Aturan ProGuard / R8
    └── src/main/
        ├── AndroidManifest.xml       # Izin Internet, Bluetooth (Android 12+), & MainActivity
        ├── res/                      # Resource XML (Tema gelap, String, Icon Adaptive)
        └── java/com/warungsenja/kasirterangbulan/
            ├── KasirApplication.kt   # Application class pengelola AppContainer
            ├── MainActivity.kt       # Entry point Jetpack Compose & Runtime Permission
            ├── di/
            │   └── AppContainer.kt   # Dependency Injection Container bersih tanpa KAPT/Hilt
            ├── domain/
            │   ├── model/
            │   │   └── Models.kt     # Entitas bisnis (ProductItem, ProductVariant, TransactionRecord, StoreSettings, dll.)
            │   ├── repository/
            │   │   └── PosRepository.kt # Kontrak interface repository
            │   └── usecase/
            │       └── PosUseCases.kt   # Use Cases (CalculateDashboardStats, Checkout, BackupRestore, ResetAllData)
            ├── data/
            │   ├── local/
            │   │   ├── InitialData.kt            # Data awal 11 menu multi-varian Warung Senja
            │   │   └── LocalStorageDataSource.kt # Persistensi JSON lokal via SharedPreferences
            │   ├── remote/
            │   │   └── GoogleSheetsDataSource.kt # Klien HTTP Google Apps Script & Template Script
            │   ├── hardware/
            │   │   └── ThermalPrinterManager.kt  # Driver Bluetooth ESC/POS 58mm/80mm, Raster Logo & PrintManager
            │   └── repository/
            │       └── PosRepositoryImpl.kt      # Implementasi PosRepository
            └── presentation/
                ├── theme/
                │   └── Theme.kt          # Palet warna Kuning Emas & Hitam (Zinc-950 & Yellow-400)
                ├── util/
                │   └── Formatter.kt      # Format Rupiah, Waktu WIB, & Generator Kode Transaksi
                ├── viewmodel/
                │   └── PosViewModel.kt   # StateFlow UI State & koordinasi bisnis
                ├── components/
                │   └── RoleHeader.kt     # Header navigasi Owner & Kasir + Indikator Cloud Sync
                └── screens/
                    ├── LoginScreen.kt        # Login PIN Kasir (1234) & Owner (9999) + Keypad Interaktif
                    ├── KasirScreen.kt        # Layar POS Kasir, Multi-Varian, Keranjang, Kalkulator Kembalian & Struk
                    ├── DashboardScreen.kt    # Ringkasan Omzet, Modal HPP, Laba Bersih & Grafik 7 Hari
                    ├── InventoryScreen.kt    # Kelola Menu, Nomor Urut, & Multi-Varian Harga Modal/Jual
                    ├── HistoryScreen.kt      # Riwayat Transaksi, Cetak Ulang Struk, & Export CSV
                    ├── GoogleSheetScreen.kt  # Konfigurasi Webhook Google Sheets & Salin Kode Apps Script
                    └── SettingsScreen.kt     # Identitas Toko, Printer Bluetooth, Upload Logo Struk, Backup & Reset Data
```

---

## Cara Membuka & Build APK di Android Studio

1. **Unduh (Download/Export ZIP)** project ini dan ekstrak ke folder di komputer Anda.
2. Buka **Android Studio** -> klik **Open** -> pilih folder root project ini.
3. Tunggu proses **Gradle Sync** selesai (otomatis mengunduh Gradle `8.10.2` dan dependensi resmi Google/AndroidX).
4. Untuk menjalankan di HP / Emulator:
   - Klik tombol **Run 'app'** (ikon segitiga hijau di toolbar atas).
5. Untuk membuat file installer **APK**:
   - Klik menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - File APK siap pasang berada di: `app/build/outputs/apk/debug/app-debug.apk`.
