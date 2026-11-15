package com.rupeshbhurke.househunter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rupeshbhurke.househunter.feature.collections.CollectionsScreen
import com.rupeshbhurke.househunter.feature.entries.EntryListScreen
import com.rupeshbhurke.househunter.feature.entry.EntryDetailScreen
import com.rupeshbhurke.househunter.feature.entry.EntryEditScreen
import kotlinx.serialization.Serializable

@Serializable
data object CollectionsRoute

@Serializable
data class EntryListRoute(val collectionId: Long)

@Serializable
data class EntryDetailRoute(val entryId: Long)

@Serializable
data class EntryEditRoute(val entryId: Long)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CollectionsRoute) {
        composable<CollectionsRoute> {
            CollectionsScreen(onOpenCollection = { navController.navigate(EntryListRoute(it)) })
        }
        composable<EntryListRoute> {
            EntryListScreen(
                onBack = navController::popBackStack,
                onOpenEntry = { navController.navigate(EntryDetailRoute(it)) },
                // A new flat opens straight in the edit form; saving returns to the list.
                onEntryCreated = { navController.navigate(EntryEditRoute(it)) },
            )
        }
        composable<EntryDetailRoute> {
            EntryDetailScreen(
                onBack = navController::popBackStack,
                onEdit = { navController.navigate(EntryEditRoute(it)) },
            )
        }
        composable<EntryEditRoute> {
            EntryEditScreen(onDone = navController::popBackStack)
        }
    }
}
