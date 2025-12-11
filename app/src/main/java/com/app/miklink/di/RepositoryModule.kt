package com.app.miklink.di

import com.app.miklink.data.repository.RouteManager
import com.app.miklink.data.repository.RouteManagerImpl
import com.app.miklink.data.repository.BackupManager
import com.app.miklink.data.repository.BackupManagerImpl
import com.app.miklink.data.repository.TransactionRunner
import com.app.miklink.data.repository.RoomTransactionRunner
import com.app.miklink.data.db.AppDatabase
import dagger.Provides
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
// removed duplicate import
import com.app.miklink.domain.usecase.backup.ImportBackupUseCase
import com.app.miklink.domain.usecase.backup.ImportBackupUseCaseImpl
import com.app.miklink.data.io.FileReader
import com.app.miklink.data.io.ContentResolverFileReader
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRouteManager(impl: RouteManagerImpl): RouteManager
    @Binds
    abstract fun bindBackupManager(impl: BackupManagerImpl): BackupManager

    @Binds
    abstract fun bindImportBackupUseCase(impl: ImportBackupUseCaseImpl): ImportBackupUseCase

    companion object {
        @Provides
        fun provideTransactionRunner(db: AppDatabase): TransactionRunner = RoomTransactionRunner(db)

        @Provides
        fun provideContentResolverFileReader(@ApplicationContext context: Context): FileReader = ContentResolverFileReader(context)
    }
}
