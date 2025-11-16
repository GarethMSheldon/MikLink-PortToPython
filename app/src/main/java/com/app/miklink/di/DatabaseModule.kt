package com.app.miklink.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.miklink.data.db.AppDatabase
import com.app.miklink.data.db.model.TestProfile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Migrazione v7 → v8: aggiunta colonna pingCount a test_profiles
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE test_profiles ADD COLUMN pingCount INTEGER NOT NULL DEFAULT 4")
        }
    }

    // Migrazione v8 → v9: aggiunta campi Speed Test a clients
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerAddress TEXT")
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerUser TEXT")
            database.execSQL("ALTER TABLE clients ADD COLUMN speedTestServerPassword TEXT")
        }
    }

    // Migrazione v9 → v10: aggiunta flag runSpeedTest a test_profiles
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE test_profiles ADD COLUMN runSpeedTest INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // Use a Provider to avoid circular dependency
        testProfileDaoProvider: Provider<com.app.miklink.data.db.dao.TestProfileDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "miklink-db"
        )
        .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Coroutine scope for the callback
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    addDefaultProfiles(testProfileDaoProvider.get())
                }
            }
        })
        .build()
    }

    private suspend fun addDefaultProfiles(testProfileDao: com.app.miklink.data.db.dao.TestProfileDao) {
        testProfileDao.insert(
            TestProfile(
                profileName = "Full Test",
                profileDescription = "TDR, Link, LLDP, and Ping",
                runTdr = true,
                runLinkStatus = true,
                runLldp = true,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY",
                pingTarget2 = "8.8.8.8"
            )
        )
        testProfileDao.insert(
            TestProfile(
                profileName = "Quick Test",
                profileDescription = "Link status and Ping",
                runTdr = false,
                runLinkStatus = true,
                runLldp = false,
                runPing = true,
                pingTarget1 = "DHCP_GATEWAY"
            )
        )
    }

    @Provides
    fun provideClientDao(db: AppDatabase) = db.clientDao()

    @Provides
    fun provideProbeConfigDao(db: AppDatabase) = db.probeConfigDao()

    @Provides
    fun provideReportDao(db: AppDatabase) = db.reportDao()

    @Provides
    fun provideTestProfileDao(db: AppDatabase) = db.testProfileDao()
}