package com.app.miklink.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class ThemeConfig {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

enum class IdNumberingStrategy {
    CONTINUOUS_INCREMENT,  // Default: sempre avanti
    FILL_GAPS             // Riempie i buchi
}

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val THEME_KEY = stringPreferencesKey("theme_config")
    private val ID_NUMBERING_STRATEGY_KEY = stringPreferencesKey("id_numbering_strategy")

    val themeConfig: Flow<ThemeConfig> = dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeConfig.FOLLOW_SYSTEM.name
            try {
                ThemeConfig.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeConfig.FOLLOW_SYSTEM
            }
        }

    val idNumberingStrategy: Flow<IdNumberingStrategy> = dataStore.data
        .map { preferences ->
            val strategyName = preferences[ID_NUMBERING_STRATEGY_KEY] ?: IdNumberingStrategy.CONTINUOUS_INCREMENT.name
            try {
                IdNumberingStrategy.valueOf(strategyName)
            } catch (e: IllegalArgumentException) {
                IdNumberingStrategy.CONTINUOUS_INCREMENT
            }
        }

    suspend fun setTheme(themeConfig: ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeConfig.name
        }
    }

    suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy) {
        dataStore.edit { preferences ->
            preferences[ID_NUMBERING_STRATEGY_KEY] = strategy.name
        }
    }
}
