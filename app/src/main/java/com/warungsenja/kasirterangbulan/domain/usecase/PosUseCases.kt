package com.warungsenja.kasirterangbulan.domain.usecase

import com.warungsenja.kasirterangbulan.domain.model.BackupPayload
import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.DashboardStats
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.SyncStatus
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.domain.repository.PosRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculateDashboardStatsUseCase {
    fun getItemUnitCost(item: CartItem, products: List<ProductItem>): Long {
        if (item.unitCost > 0L) return item.unitCost
        val product = products.firstOrNull {
            it.id == item.productId || it.barcode.equals(item.barcode, ignoreCase = true)
        } ?: return 0L
        val matchedVariant = product.variants.firstOrNull {
            it.name.equals(item.category, ignoreCase = true)
        }
        return matchedVariant?.purchasePrice ?: product.purchasePrice
    }

    fun getTransactionCost(trx: TransactionRecord, products: List<ProductItem>): Long {
        return trx.items.sumOf { item -> getItemUnitCost(item, products) * item.qty }
    }

    operator fun invoke(
        products: List<ProductItem>,
        transactions: List<TransactionRecord>
    ): DashboardStats {
        val salesTransactions = transactions.filter { it.type == TransactionType.OUT }
        val totalSalesQty = salesTransactions.sumOf { it.totalQty }
        val totalRevenue = salesTransactions.sumOf { it.totalAmount }
        val totalModal = salesTransactions.sumOf { getTransactionCost(it, products) }
        val totalLaba = totalRevenue - totalModal

        return DashboardStats(
            totalMenu = products.size,
            totalSalesCount = salesTransactions.size,
            totalSalesQty = totalSalesQty,
            totalRevenue = totalRevenue,
            totalModal = totalModal,
            totalLaba = totalLaba
        )
    }
}

class ResetAllDataUseCase(private val repository: PosRepository) {
    operator fun invoke(): BackupPayload = repository.resetAllData()
}

class BackupRestoreUseCase(private val repository: PosRepository) {
    fun exportJson(): String = repository.exportBackupJson()
    fun restoreJson(rawJson: String): BackupPayload = repository.restoreBackupJson(rawJson)
}

class CheckoutTransactionUseCase(private val repository: PosRepository) {
    suspend operator fun invoke(
        newTransaction: TransactionRecord,
        currentProducts: List<ProductItem>,
        currentTransactions: List<TransactionRecord>
    ): Pair<List<ProductItem>, List<TransactionRecord>> {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        val updatedProducts = currentProducts.map { product ->
            val matchingQty = newTransaction.items
                .filter { it.productId == product.id }
                .sumOf { it.qty }
            if (matchingQty == 0) {
                product
            } else {
                product.copy(
                    stock = (product.stock - matchingQty).coerceAtLeast(0),
                    lastUpdated = nowIso
                )
            }
        }
        repository.saveProducts(updatedProducts)

        var finalTrx = newTransaction
        val initialList = listOf(finalTrx) + currentTransactions
        repository.saveTransactions(initialList)

        val sheetConfig = repository.getSheetConfig()
        if (sheetConfig.autoSync && sheetConfig.webhookUrl.isNotBlank()) {
            val syncRes = repository.syncTransactionToSheet(
                webhookUrl = sheetConfig.webhookUrl,
                transaction = finalTrx,
                products = updatedProducts
            )
            finalTrx = if (syncRes.success) {
                finalTrx.copy(
                    syncStatus = SyncStatus.SYNCED,
                    syncedAt = nowIso,
                    syncError = null
                )
            } else {
                finalTrx.copy(
                    syncStatus = SyncStatus.FAILED,
                    syncError = syncRes.message
                )
            }
            val syncedList = listOf(finalTrx) + currentTransactions
            repository.saveTransactions(syncedList)
            return updatedProducts to syncedList
        }

        return updatedProducts to initialList
    }
}
