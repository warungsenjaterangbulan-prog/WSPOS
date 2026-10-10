package com.warungsenja.kasirterangbulan.presentation.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.warungsenja.kasirterangbulan.domain.model.SyncStatus
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.presentation.theme.Amber500
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
import com.warungsenja.kasirterangbulan.presentation.util.Formatter

@Composable
fun HistoryScreen(
    transactions: List<TransactionRecord>,
    getTransactionModal: (TransactionRecord) -> Long,
    onPrintReceipt: (Activity?, TransactionRecord) -> Pair<Boolean, String>,
    onRetrySync: (TransactionRecord, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var search by remember { mutableStateOf("") }
    var syncFilter by remember { mutableStateOf("ALL") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var feedbackBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val filtered = remember(transactions, search, syncFilter) {
        val q = search.trim().lowercase()
        transactions.filter { trx ->
            val matchQuery = q.isEmpty() ||
                trx.code.lowercase().contains(q) ||
                trx.recipientOrSupplier.lowercase().contains(q) ||
                trx.operatorName.lowercase().contains(q) ||
                trx.items.any { it.name.lowercase().contains(q) }
            val matchSync = syncFilter == "ALL" || trx.syncStatus.rawValue == syncFilter
            matchQuery && matchSync
        }
    }

    val totalOmzetFiltered = remember(filtered) { filtered.sumOf { it.totalAmount } }
    val totalModalFiltered = remember(filtered) { filtered.sumOf { getTransactionModal(it) } }
    val totalProfitFiltered = totalOmzetFiltered - totalModalFiltered

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Riwayat Transaksi Penjualan",
                    color = Zinc100,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Laporan lengkap omzet, modal HPP, laba bersih, dan cetak ulang struk.",
                    color = Zinc400,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val header = "Kode,Waktu,Kasir,Pelanggan,TotalPorsi,Omzet,Modal,Laba,StatusSync\n"
                    val rows = filtered.joinToString("\n") { trx ->
                        val modal = getTransactionModal(trx)
                        val profit = trx.totalAmount - modal
                        "${trx.code},\"${Formatter.formatDateTime(trx.timestamp)}\",\"${trx.operatorName}\",\"${trx.recipientOrSupplier}\",${trx.totalQty},${trx.totalAmount},$modal,$profit,${trx.syncStatus.rawValue}"
                    }
                    val csvContent = header + rows
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Transaksi Warung Senja")
                        putExtra(Intent.EXTRA_TEXT, csvContent)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan / Simpan CSV Laporan"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Zinc800,
                    contentColor = Yellow400
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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

        // Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMiniBox(
                label = "OMZET (${filtered.size} TRX)",
                value = Formatter.formatRupiah(totalOmzetFiltered),
                valueColor = Yellow400,
                modifier = Modifier.weight(1f)
            )
            SummaryMiniBox(
                label = "TOTAL MODAL",
                value = Formatter.formatRupiah(totalModalFiltered),
                valueColor = Zinc300,
                modifier = Modifier.weight(1f)
            )
            SummaryMiniBox(
                label = "LABA BERSIH",
                value = Formatter.formatRupiah(totalProfitFiltered),
                valueColor = Emerald400,
                modifier = Modifier.weight(1f)
            )
        }

        // Search & Filter
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Cari kode struk, pelanggan, menu...", color = Zinc500, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Zinc500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Yellow400,
                unfocusedBorderColor = Zinc800,
                focusedContainerColor = Zinc900,
                unfocusedContainerColor = Zinc900,
                focusedTextColor = Zinc100,
                unfocusedTextColor = Zinc100
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "ALL" to "Semua (${transactions.size})",
                "synced" to "Tersinkron",
                "local" to "Lokal",
                "failed" to "Gagal Sync"
            ).forEach { (key, label) ->
                val selected = syncFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Yellow400 else Zinc900)
                        .border(1.dp, if (selected) Yellow300 else Zinc800, RoundedCornerShape(10.dp))
                        .clickable { syncFilter = key }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Zinc950 else Zinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada transaksi yang tercatat.", color = Zinc500, fontSize = 13.sp)
            }
        } else {
            filtered.forEach { trx ->
                val modal = getTransactionModal(trx)
                val profit = trx.totalAmount - modal
                val isExpanded = expandedId == trx.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc900.copy(alpha = 0.85f))
                        .border(1.dp, if (isExpanded) Yellow400.copy(alpha = 0.5f) else Zinc800, RoundedCornerShape(16.dp))
                        .clickable { expandedId = if (isExpanded) null else trx.id }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trx.code,
                                    color = Yellow400,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                SyncBadge(status = trx.syncStatus)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${trx.recipientOrSupplier} • Kasir: ${trx.operatorName}",
                                color = Zinc100,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Formatter.formatDateTime(trx.timestamp),
                                color = Zinc500,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatter.formatRupiah(trx.totalAmount),
                                color = Yellow400,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Laba: +${Formatter.formatRupiah(profit)}",
                                color = Emerald400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Zinc950)
                                .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "RINCIAN ITEM (${trx.totalQty} Porsi):",
                                color = Zinc400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            trx.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.qty}x ${item.name}",
                                        color = Zinc100,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = Formatter.formatRupiah(item.subtotal),
                                        color = Yellow300,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (trx.note.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${trx.note}",
                                    color = Zinc400,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val res = onPrintReceipt(activity, trx)
                                    feedbackBanner = res
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Yellow400,
                                    contentColor = Zinc950
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cetak Struk", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            if (trx.syncStatus != SyncStatus.SYNCED) {
                                Button(
                                    onClick = {
                                        onRetrySync(trx) { ok, msg ->
                                            feedbackBanner = ok to msg
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Zinc800,
                                        contentColor = Zinc100
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Sheets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
private fun SummaryMiniBox(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Zinc900)
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = Zinc500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SyncBadge(status: SyncStatus) {
    val (bg, textCol, label) = when (status) {
        SyncStatus.SYNCED -> Triple(Emerald500.copy(alpha = 0.15f), Emerald300, "Tersinkron")
        SyncStatus.PENDING -> Triple(Amber500.copy(alpha = 0.15f), Amber500, "Pending")
        SyncStatus.LOCAL -> Triple(Amber500.copy(alpha = 0.15f), Amber500, "Lokal")
        SyncStatus.FAILED -> Triple(Red500.copy(alpha = 0.15f), Red400, "Gagal Sync")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status == SyncStatus.SYNCED) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = textCol, modifier = Modifier.size(10.dp))
        }
        Text(text = label, color = textCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
