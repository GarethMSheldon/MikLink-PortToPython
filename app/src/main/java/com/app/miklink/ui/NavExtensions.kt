package com.app.miklink.ui

import androidx.navigation.NavController

/**
 * Navigazione sicura alla Dashboard: evita duplicazioni nello stack e ripristina lo stato.
 */
fun NavController.navigateDashboard() {
    // Se siamo già sulla dashboard, non fare nulla
    if (this.currentDestination?.route == "dashboard") return
    this.navigate("dashboard") {
        // Torna allo start della graph preservando stato
        popUpTo(this@navigateDashboard.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

