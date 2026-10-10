package com.warungsenja.kasirterangbulan.presentation.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.warungsenja.kasirterangbulan.domain.model.BluetoothDeviceItem

import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald300
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald400
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald500
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Red500
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow300
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc500
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import java.io.ByteArrayOutputStream

@Composable
fun SettingsScreen(
    settings: StoreSettings,
    pairedDevices: List<BluetoothDeviceItem>,
    onSaveSettings: (StoreSettings) -> Unit,
    onScanPairedDevices: () -> List<BluetoothDeviceItem>,
    onConnectBluetooth: (BluetoothDeviceItem) -> Pair<Boolean, String>,
    onDisconnectBluetooth: () -> Pair<Boolean, String>,
    onPrintTestReceipt: (Activity?) -> Pair<Boolean, String>,
    onExportBackupJson: () -> String,
    onRestoreBackupJson: (String) -> Result<Pair<Int, Int>>,
    onResetAllData: () -> Unit,
    onRequestBluetoothPermission: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var storeName by remember(settings.storeName) { mutableStateOf(settings.storeName) }
    var storeAddress by remember(settings.storeAddress) { mutableStateOf(settings.storeAddress) }
    var storePhone by remember(settings.storePhone) { mutableStateOf(settings.storePhone) }
    var operationalHours by remember(settings.operationalHours) { mutableStateOf(settings.operationalHours) }
    var receiptFooter by remember(settings.receiptFooter) { mutableStateOf(settings.receiptFooter) }
    var ownerPin by remember(settings.ownerPin) { mutableStateOf(settings.ownerPin) }
    var kasirPin by remember(settings.kasirPin) { mutableStateOf(settings.kasirPin) }
    var enableScanSound by remember(settings.enableScanSound) { mutableStateOf(settings.enableScanSound) }

    var printerState by remember(settings.printer) { mutableStateOf(settings.printer) }
    var feedbackBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var restoreJsonText by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        val maxWidth = 240
                        val ratio = maxWidth.toFloat() / original.width.toFloat().coerceAtLeast(1f)
                        val targetW = if (original.width > maxWidth) maxWidth else original.width
                        val targetH = if (original.width > maxWidth) (original.height * ratio).toInt().coerceAtLeast(1) else original.height
                        val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
                        val base64 = "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        val updatedPrinter = printerState.copy(receiptLogoDataUrl = base64, showReceiptLogo = true)
                        printerState = updatedPrinter
                        onSaveSettings(settings.copy(printer = updatedPrinter))
                        feedbackBanner = true to "Logo struk berhasil diunggah!"
                    }
                }
            } catch (e: Exception) {
                feedbackBanner = false to "Gagal memuat gambar logo: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Pengaturan Toko, Printer & Data",
                color = Zinc100,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Atur profil Warung Senja, printer thermal Bluetooth ESC/POS, PIN akses, serta backup/reset data.",
                color = Zinc400,
                fontSize = 12.sp
            )
        }

        if (feedbackBanner != null) {
            val (ok, msg) = feedbackBanner!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ok) Emerald500.copy(alpha = 0.12f) else Red500.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        if (ok) Emerald500.copy(alpha = 0.35f) else Red500.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = msg,
                    color = if (ok) Emerald300 else Red400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Section 1: Identitas Toko & PIN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Zinc900.copy(alpha = 0.85f))
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "1. Identitas Toko & Keamanan PIN",
                color = Yellow400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Nama Usaha", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text("No. Telepon / WA", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = operationalHours,
                    onValueChange = { operationalHours = it },
                    label = { Text("Jam Operasional / Info", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = storeAddress,
                onValueChange = { storeAddress = it },
                label = { Text("Alamat Lengkap (Tampil di Struk)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                label = { Text("Catatan Kaki Struk (Footer)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ownerPin,
                    onValueChange = { ownerPin = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("PIN Owner", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = kasirPin,
                    onValueChange = { kasirPin = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("PIN Kasir", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Suara Beep Saat Checkout Kasir", color = Zinc100, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = enableScanSound,
                    onCheckedChange = { enableScanSound = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Zinc950, checkedTrackColor = Yellow400)
                )
            }

            Button(
                onClick = {
                    val next = settings.copy(
                        storeName = storeName.trim().ifBlank { "Warung Senja Terang Bulan" },
                        storeAddress = storeAddress.trim(),
                        storePhone = storePhone.trim(),
                        operationalHours = operationalHours.trim(),
                        receiptFooter = receiptFooter.trim(),
                        ownerPin = ownerPin.ifBlank { "1234" },
                        kasirPin = kasirPin.ifBlank { "0000" },
                        enableScanSound = enableScanSound,
                        printer = printerState
                    )
                    onSaveSettings(next)
                    feedbackBanner = true to "Identitas toko & PIN berhasil disimpan!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = Yellow400, contentColor = Zinc950),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Identitas & PIN", fontWeight = FontWeight.ExtraBold)
            }
        }

        // Section 2: Pengaturan Printer Thermal Bluetooth & Struk
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Zinc900.copy(alpha = 0.85f))
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "2. Printer Thermal Bluetooth ESC/POS & Struk",
                color = Yellow400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Mode Koneksi Printer
            Text("Metode Cetak Struk:", color = Zinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "bluetooth" to "Bluetooth Thermal (ESC/POS)",
                    "browser" to "Android Print / PDF"
                ).forEach { (mode, label) ->
                    val selected = printerState.connectionMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Yellow400 else Zinc950)
                            .border(1.dp, if (selected) Yellow300 else Zinc800, RoundedCornerShape(10.dp))
                            .clickable {
                                val nextPrinter = printerState.copy(connectionMode = mode)
                                printerState = nextPrinter
                                onSaveSettings(settings.copy(printer = nextPrinter))
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Zinc950 else Zinc300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Ukuran Kertas
            Text("Ukuran Kertas Thermal:", color = Zinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "58mm" to "58mm (32 Karakter)",
                    "80mm" to "80mm (48 Karakter)"
                ).forEach { (pw, label) ->
                    val selected = printerState.paperSize == pw
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Yellow400 else Zinc950)
                            .border(1.dp, if (selected) Yellow300 else Zinc800, RoundedCornerShape(10.dp))
                            .clickable {
                                val nextPrinter = printerState.copy(paperSize = pw)
                                printerState = nextPrinter
                                onSaveSettings(settings.copy(printer = nextPrinter))
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Zinc950 else Zinc300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cetak Struk Otomatis Setelah Checkout", color = Zinc100, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = printerState.autoPrintAfterCheckout,
                    onCheckedChange = {
                        val nextPrinter = printerState.copy(autoPrintAfterCheckout = it)
                        printerState = nextPrinter
                        onSaveSettings(settings.copy(printer = nextPrinter))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Zinc950, checkedTrackColor = Yellow400)
                )
            }

            // Bluetooth Device Picker
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc950)
                    .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Perangkat Bluetooth Terpilih:", color = Zinc400, fontSize = 11.sp)
                        Text(
                            text = if (printerState.bluetoothDeviceName.isNotBlank())
                                "${printerState.bluetoothDeviceName} (${printerState.bluetoothDeviceAddress})"
                            else "Belum dipilih",
                            color = if (printerState.bluetoothDeviceName.isNotBlank()) Emerald400 else Zinc500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            onRequestBluetoothPermission()
                            val found = onScanPairedDevices()
                            feedbackBanner = true to "Ditemukan ${found.size} perangkat Bluetooth yang sudah dipairing."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Yellow400),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Paired", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "Tip: Pairing printer Bluetooth terlebih dahulu di Pengaturan Bluetooth HP Android Anda, lalu klik 'Scan Paired'.",
                        color = Zinc500,
                        fontSize = 11.sp
                    )
                } else {
                    pairedDevices.forEach { dev ->
                        val isSelected = printerState.bluetoothDeviceAddress.equals(dev.address, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Emerald500.copy(alpha = 0.12f) else Zinc900)
                                .border(
                                    1.dp,
                                    if (isSelected) Emerald500.copy(alpha = 0.4f) else Zinc800,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val res = onConnectBluetooth(dev)
                                    printerState = printerState.copy(
                                        bluetoothDeviceName = dev.name,
                                        bluetoothDeviceAddress = dev.address
                                    )
                                    feedbackBanner = res
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isSelected) Emerald400 else Yellow400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(dev.name, color = Zinc100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(dev.address, color = Zinc500, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text(
                                text = if (isSelected) "Terpilih" else "Hubungkan",
                                color = if (isSelected) Emerald300 else Yellow400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Upload Logo Struk
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc950)
                    .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Logo Struk Thermal (Monokrom / Hitam Putih)", color = Zinc100, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = printerState.showReceiptLogo,
                        onCheckedChange = {
                            val nextPrinter = printerState.copy(showReceiptLogo = it)
                            printerState = nextPrinter
                            onSaveSettings(settings.copy(printer = nextPrinter))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Zinc950, checkedTrackColor = Yellow400)
                    )
                }

                if (printerState.receiptLogoDataUrl.isNotBlank()) {
                    val bmp = remember(printerState.receiptLogoDataUrl) {
                        try {
                            val raw = printerState.receiptLogoDataUrl.substringAfter("base64,", printerState.receiptLogoDataUrl)
                            val bytes = Base64.decode(raw, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bmp != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Logo Struk",
                                modifier = Modifier.height(64.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Yellow400),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Gambar Logo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (printerState.receiptLogoDataUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val nextPrinter = printerState.copy(receiptLogoDataUrl = "")
                                printerState = nextPrinter
                                onSaveSettings(settings.copy(printer = nextPrinter))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Red500.copy(alpha = 0.15f), contentColor = Red400),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val res = onPrintTestReceipt(activity)
                    feedbackBanner = res
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Zinc950),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cetak Tes Struk Sekarang", fontWeight = FontWeight.ExtraBold)
            }
        }

        // Section 3: Backup, Restore & Reset Data
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Zinc900.copy(alpha = 0.85f))
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "3. Backup, Restore & Reset Data Aplikasi",
                color = Yellow400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Salin JSON backup untuk menyimpan cadangan menu & riwayat transaksi, atau lakukan Reset Data untuk mengosongkan menu dan transaksi.",
                color = Zinc400,
                fontSize = 11.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val json = onExportBackupJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("BackupWarungSenja", json))
                        feedbackBanner = true to "Data Backup JSON berhasil disalin ke Clipboard!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Yellow400),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Backup JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showRestoreDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Emerald300),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { showResetConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red500.copy(alpha = 0.15f),
                    contentColor = Red400
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Data (Kosongkan Menu & Riwayat Transaksi)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Restore JSON Dialog
    if (showRestoreDialog) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Restore Data dari Backup JSON", color = Zinc100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = restoreJsonText,
                    onValueChange = { restoreJsonText = it },
                    placeholder = { Text("Tempelkan teks JSON backup di sini...", color = Zinc500, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showRestoreDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc300),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val res = onRestoreBackupJson(restoreJsonText)
                            res.onSuccess { (prodCount, trxCount) ->
                                feedbackBanner = true to "Restore berhasil! ($prodCount menu & $trxCount transaksi dipulihkan)"
                                showRestoreDialog = false
                            }.onFailure { err ->
                                feedbackBanner = false to "Format JSON tidak valid: ${err.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Yellow400, contentColor = Zinc950),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pulihkan", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Zinc900)
                    .border(1.dp, Red500.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Red400)
                    Text("Konfirmasi Reset Data", color = Zinc100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Tindakan ini akan mengosongkan seluruh daftar menu dan riwayat transaksi menjadi 0 (kosong), serta mengembalikan pengaturan identitas toko ke awal. Lanjutkan?",
                    color = Zinc300,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showResetConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc300),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onResetAllData()
                            showResetConfirmDialog = false
                            feedbackBanner = true to "Seluruh daftar menu & riwayat transaksi berhasil dikosongkan!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red500, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ya, Reset Data", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
