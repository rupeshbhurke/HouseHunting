package com.rupeshbhurke.househunter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rupeshbhurke.househunter.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
    }
}
