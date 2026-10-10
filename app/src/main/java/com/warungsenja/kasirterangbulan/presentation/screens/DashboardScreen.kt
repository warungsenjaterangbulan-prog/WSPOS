package com.warungsenja.kasirterangbulan.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungsenja.kasirterangbulan.domain.model.DashboardStats
import com.warungsenja.kasirterangbulan.domain.model.OwnerTab
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald400
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400
import com.warungsenja.kasirterangbulan.presentation.theme.YellowAmberGradient
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import com.warungsenja.kasirterangbulan.presentation.util.Formatter

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    settings: StoreSettings,
    transactions: List<TransactionRecord>,
    getTransactionModal: (TransactionRecord) -> Long,
    onNavigateTab: (OwnerTab) -> Unit
) {
    val salesTransactions = transactions.filter { it.type == TransactionType.OUT }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Store Hero Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(YellowAmberGradient)
                .padding(18.dp)
        ) {
            Text(
                text = settings.storeName,
                color = Zinc950,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${settings.storeAddress} · Telp/WA: ${settings.storePhone} · ${settings.operationalHours}",
                color = Zinc950.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigateTab(OwnerTab.KASIR) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Zinc950,
                        contentColor = Yellow400
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Buka Kasir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onNavigateTab(OwnerTab.INVENTORY) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Zinc950.copy(alpha = 0.15f),
                        contentColor = Zinc950
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Daftar Menu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5 Summary Metric Cards: Omzet, Modal, Laba, Terjual, Total Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                label = "Omzet",
                value = Formatter.formatRupiah(stats.totalRevenue),
                valueColor = Yellow400,
                subtitle = "${stats.totalSalesCount} transaksi penjualan"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountBalanceWallet,
                label = "Modal",
                value = Formatter.formatRupiah(stats.totalModal),
                valueColor = Zinc100,
                subtitle = "Total HPP / harga beli"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Laba",
                value = Formatter.formatRupiah(stats.totalLaba),
                valueColor = if (stats.totalLaba >= 0) Emerald400 else Red400,
                subtitle = "Omzet dikurangi modal",
                borderColor = Emerald400.copy(alpha = 0.35f)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingBag,
                label = "Terjual",
                value = "${stats.totalSalesQty} Porsi",
                valueColor = Zinc100,
                subtitle = "Akumulasi porsi keluar"
            )
        }

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Inventory2,
            label = "Total Menu",
            value = "${stats.totalMenu} Menu",
            valueColor = Zinc100,
            subtitle = "Varian menu aktif"
        )

        // Recent Transactions Table with Modal & Laba Breakdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Zinc800, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru (Rincian Modal & Laba)",
                    color = Zinc100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateTab(OwnerTab.HISTORY) }
                ) {
                    Text(
                        text = "Semua Riwayat",
                        color = Yellow400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Yellow400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (salesTransactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi tercatat.",
                    color = Zinc400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    salesTransactions.take(8).forEach { trx ->
                        val trxModal = getTransactionModal(trx)
                        val trxLaba = trx.totalAmount - trxModal
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Zinc950)
                                .border(1.dp, Zinc800, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(125.dp)) {
                                Text(
                                    text = trx.code,
                                    color = Yellow400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Formatter.formatDateTime(trx.timestamp),
                                    color = Zinc400,
                                    fontSize = 10.sp
                                )
                            }
                            Column(modifier = Modifier.width(110.dp)) {
                                Text(
                                    text = trx.recipientOrSupplier,
                                    color = Zinc100,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${trx.totalQty} pcs",
                                    color = Zinc400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                            Column(modifier = Modifier.width(100.dp)) {
                                Text("Modal", color = Zinc400, fontSize = 10.sp)
                                Text(
                                    text = Formatter.formatRupiah(trxModal),
                                    color = Zinc300,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Column(modifier = Modifier.width(105.dp)) {
                                Text("Penjualan", color = Zinc400, fontSize = 10.sp)
                                Text(
                                    text = Formatter.formatRupiah(trx.totalAmount),
                                    color = Yellow400,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Column(modifier = Modifier.width(105.dp)) {
                                Text("Laba", color = Zinc400, fontSize = 10.sp)
                                Text(
                                    text = "${if (trxLaba >= 0) "+" else ""}${Formatter.formatRupiah(trxLaba)}",
                                    color = if (trxLaba >= 0) Emerald400 else Red400,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    borderColor: Color = Zinc800
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Zinc900)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Yellow400),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Zinc950,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Zinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Zinc400,
            fontSize = 10.sp
        )
    }
}
