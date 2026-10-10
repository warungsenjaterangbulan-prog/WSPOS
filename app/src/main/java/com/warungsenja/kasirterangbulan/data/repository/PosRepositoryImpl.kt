package com.warungsenja.kasirterangbulan.data.repository

import com.warungsenja.kasirterangbulan.data.local.LocalStorageDataSource
import com.warungsenja.kasirterangbulan.data.remote.GoogleSheetsDataSource
import com.warungsenja.kasirterangbulan.domain.model.BackupPayload
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.SyncResult
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.UserProfile
import com.warungsenja.kasirterangbulan.domain.repository.PosRepository

class PosRepositoryImpl(
    private val localDataSource: LocalStorageDataSource,
    private val remoteDataSource: GoogleSheetsDataSource
) : PosRepository {

    override fun getProducts(): List<ProductItem> = localDataSource.getProducts()

    override fun saveProducts(products: List<ProductItem>) = localDataSource.saveProducts(products)

    override fun getTransactions(): List<TransactionRecord> = localDataSource.getTransactions()

    override fun saveTransactions(transactions: List<TransactionRecord>) =
        localDataSource.saveTransactions(transactions)

    override fun getSettings(): StoreSettings = localDataSource.getSettings()

    override fun saveSettings(settings: StoreSettings) = localDataSource.saveSettings(settings)

    override fun getSheetConfig(): GoogleSheetConfig = localDataSource.getSheetConfig()

    override fun saveSheetConfig(config: GoogleSheetConfig) = localDataSource.saveSheetConfig(config)

    override fun getCurrentUser(): UserProfile? = localDataSource.getCurrentUser()

    override fun saveCurrentUser(user: UserProfile?) = localDataSource.saveCurrentUser(user)

    override fun exportBackupJson(): String = localDataSource.exportBackupJson()

    override fun restoreBackupJson(rawJson: String): BackupPayload =
        localDataSource.restoreBackupJson(rawJson)

    override fun resetAllData(): BackupPayload = localDataSource.resetAllData()

    override suspend fun pingGoogleSheet(webhookUrl: String): SyncResult =
        remoteDataSource.pingConnection(webhookUrl)

    override suspend fun syncTransactionToSheet(
        webhookUrl: String,
        transaction: TransactionRecord,
        products: List<ProductItem>
    ): SyncResult = remoteDataSource.syncTransaction(webhookUrl, transaction, products)

    override suspend fun syncAllStockToSheet(
        webhookUrl: String,
        products: List<ProductItem>
    ): SyncResult = remoteDataSource.syncAllStock(webhookUrl, products)

    override suspend fun fetchStockFromSheet(webhookUrl: String): SyncResult =
        remoteDataSource.fetchStockFromSheet(webhookUrl)
}
