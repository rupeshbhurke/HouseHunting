package com.rupeshbhurke.househunter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rupeshbhurke.househunter.feature.capture.CaptureScreen
import com.rupeshbhurke.househunter.feature.collections.CollectionsScreen
import com.rupeshbhurke.househunter.feature.entries.EntryListScreen
import com.rupeshbhurke.househunter.feature.entry.EntryDetailScreen
import com.rupeshbhurke.househunter.feature.entry.EntryEditScreen
import com.rupeshbhurke.househunter.feature.media.MediaViewerScreen
import kotlinx.serialization.Serializable

@Serializable
data object CollectionsRoute

@Serializable
data class EntryListRoute(val collectionId: Long)

@Serializable
data class EntryDetailRoute(val entryId: Long)

@Serializable
data class EntryEditRoute(val entryId: Long)

@Serializable
data class CaptureRoute(val entryId: Long, val videoNote: Boolean = false)

@Serializable
data class MediaViewerRoute(val entryId: Long, val startIndex: Int)

@Composable
fun AppNavHost(openEntryId: Long? = null) {
    val navController = rememberNavController()
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openEntryId) {
        if (openEntryId != null && !opened) {
            opened = true
            navController.navigate(EntryDetailRoute(openEntryId))
        }
    }
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
                onCapture = { navController.navigate(CaptureRoute(it)) },
                onVideoNote = { navController.navigate(CaptureRoute(it, videoNote = true)) },
                onOpenMedia = { entryId, index -> navController.navigate(MediaViewerRoute(entryId, index)) },
            )
        }
        composable<CaptureRoute> {
            CaptureScreen(onClose = navController::popBackStack)
        }
        composable<MediaViewerRoute> {
            MediaViewerScreen(onBack = navController::popBackStack)
        }
        composable<EntryEditRoute> {
            EntryEditScreen(onDone = navController::popBackStack)
        }
    }
}
