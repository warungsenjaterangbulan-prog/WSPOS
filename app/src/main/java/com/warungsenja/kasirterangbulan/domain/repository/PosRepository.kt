package com.warungsenja.kasirterangbulan.domain.repository

import com.warungsenja.kasirterangbulan.domain.model.BackupPayload
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.SyncResult
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.UserProfile

interface PosRepository {
    fun getProducts(): List<ProductItem>
    fun saveProducts(products: List<ProductItem>)

    fun getTransactions(): List<TransactionRecord>
    fun saveTransactions(transactions: List<TransactionRecord>)

    fun getSettings(): StoreSettings
    fun saveSettings(settings: StoreSettings)

    fun getSheetConfig(): GoogleSheetConfig
    fun saveSheetConfig(config: GoogleSheetConfig)

    fun getCurrentUser(): UserProfile?
    fun saveCurrentUser(user: UserProfile?)

    fun exportBackupJson(): String
    fun restoreBackupJson(rawJson: String): BackupPayload
    fun resetAllData(): BackupPayload

    suspend fun pingGoogleSheet(webhookUrl: String): SyncResult
    suspend fun syncTransactionToSheet(
        webhookUrl: String,
        transaction: TransactionRecord,
        products: List<ProductItem>
    ): SyncResult
    suspend fun syncAllStockToSheet(
        webhookUrl: String,
        products: List<ProductItem>
    ): SyncResult
    suspend fun fetchStockFromSheet(webhookUrl: String): SyncResult
}
