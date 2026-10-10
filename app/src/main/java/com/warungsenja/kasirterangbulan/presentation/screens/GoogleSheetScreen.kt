package com.warungsenja.kasirterangbulan.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungsenja.kasirterangbulan.data.remote.GoogleSheetsDataSource
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald300
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald500
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Red500
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc500
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import com.warungsenja.kasirterangbulan.presentation.util.Formatter

@Composable
fun GoogleSheetScreen(
    config: GoogleSheetConfig,
    isBusy: Boolean,
    onSaveConfig: (GoogleSheetConfig) -> Unit,
    onPingConnection: (String, (Boolean, String) -> Unit) -> Unit,
    onPushStock: (String, (Boolean, String) -> Unit) -> Unit,
    onPullStock: (String, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var webhookUrl by remember(config.webhookUrl) { mutableStateOf(config.webhookUrl) }
    var spreadsheetUrl by remember(config.spreadsheetUrl) { mutableStateOf(config.spreadsheetUrl) }
    var autoSync by remember(config.autoSync) { mutableStateOf(config.autoSync) }
    var feedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Integrasi Cloud Google Sheets",
                color = Zinc100,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sinkronkan transaksi kasir dan daftar menu Warung Senja secara real-time ke Google Spreadsheet.",
                color = Zinc400,
                fontSize = 12.sp
            )
        }

        if (feedback != null) {
            val (ok, msg) = feedback!!
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

        // Configuration Form
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
                text = "Konfigurasi URL Google Apps Script",
                color = Yellow400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { webhookUrl = it },
                label = { Text("URL Web App Google Apps Script (.../exec)", fontSize = 11.sp) },
                placeholder = { Text("https://script.google.com/macros/s/.../exec", color = Zinc500, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Yellow400,
                    unfocusedBorderColor = Zinc800,
                    focusedContainerColor = Zinc950,
                    unfocusedContainerColor = Zinc950,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = spreadsheetUrl,
                onValueChange = { spreadsheetUrl = it },
                label = { Text("Link Google Spreadsheet (Opsional)", fontSize = 11.sp) },
                placeholder = { Text("https://docs.google.com/spreadsheets/d/...", color = Zinc500, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Yellow400,
                    unfocusedBorderColor = Zinc800,
                    focusedContainerColor = Zinc950,
                    unfocusedContainerColor = Zinc950,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sinkronisasi Otomatis Saat Checkout", color = Zinc100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Kirim transaksi kasir langsung ke Google Sheets", color = Zinc400, fontSize = 11.sp)
                }
                Switch(
                    checked = autoSync,
                    onCheckedChange = { autoSync = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Zinc950,
                        checkedTrackColor = Yellow400
                    )
                )
            }

            if (config.lastSyncAt != null) {
                Text(
                    text = "Sinkronisasi terakhir: ${Formatter.formatDateTime(config.lastSyncAt)}",
                    color = Zinc400,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updated = config.copy(
                            webhookUrl = webhookUrl.trim(),
                            spreadsheetUrl = spreadsheetUrl.trim(),
                            autoSync = autoSync,
                            isConnected = webhookUrl.trim().isNotEmpty()
                        )
                        onSaveConfig(updated)
                        feedback = true to "Konfigurasi Google Sheets berhasil disimpan."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow400, contentColor = Zinc950),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Pengaturan", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onPingConnection(webhookUrl) { ok, msg ->
                            feedback = ok to msg
                        }
                    },
                    enabled = !isBusy && webhookUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc100),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tes Koneksi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onPushStock(webhookUrl) { ok, msg ->
                            feedback = ok to msg
                        }
                    },
                    enabled = !isBusy && webhookUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Yellow400),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Menu ke Sheet", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        onPullStock(webhookUrl) { ok, msg ->
                            feedback = ok to msg
                        }
                    },
                    enabled = !isBusy && webhookUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Emerald300),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tarik Menu dari Sheet", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Apps Script Template Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Zinc900.copy(alpha = 0.85f))
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kode Google Apps Script (Siap Pakai)",
                        color = Zinc100,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tempelkan di Extensions > Apps Script pada Google Sheets Anda, lalu Deploy sebagai Web App (Anyone).",
                        color = Zinc400,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("AppsScriptWarungSenja", GoogleSheetsDataSource.APPS_SCRIPT_TEMPLATE)
                        )
                        feedback = true to "Kode Google Apps Script berhasil disalin ke Clipboard!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow400, contentColor = Zinc950),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Kode", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc950)
                    .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = GoogleSheetsDataSource.APPS_SCRIPT_TEMPLATE,
                    color = Zinc300,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
