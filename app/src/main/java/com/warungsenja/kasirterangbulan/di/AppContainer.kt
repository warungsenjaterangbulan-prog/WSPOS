package com.warungsenja.kasirterangbulan.di

import android.content.Context
import com.warungsenja.kasirterangbulan.data.hardware.ThermalPrinterManager
import com.warungsenja.kasirterangbulan.data.local.LocalStorageDataSource
import com.warungsenja.kasirterangbulan.data.remote.GoogleSheetsDataSource
import com.warungsenja.kasirterangbulan.data.repository.PosRepositoryImpl
import com.warungsenja.kasirterangbulan.domain.repository.PosRepository
import com.warungsenja.kasirterangbulan.domain.usecase.BackupRestoreUseCase
import com.warungsenja.kasirterangbulan.domain.usecase.CalculateDashboardStatsUseCase
import com.warungsenja.kasirterangbulan.domain.usecase.CheckoutTransactionUseCase
import com.warungsenja.kasirterangbulan.domain.usecase.ResetAllDataUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val localDataSource = LocalStorageDataSource(appContext)
    private val remoteDataSource = GoogleSheetsDataSource()

    val repository: PosRepository = PosRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource
    )

    val printerManager = ThermalPrinterManager(appContext)

    val calculateDashboardStatsUseCase = CalculateDashboardStatsUseCase()
    val checkoutTransactionUseCase = CheckoutTransactionUseCase(repository)
    val resetAllDataUseCase = ResetAllDataUseCase(repository)
    val backupRestoreUseCase = BackupRestoreUseCase(repository)
}
