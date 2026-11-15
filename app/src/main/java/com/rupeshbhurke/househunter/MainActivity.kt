package com.rupeshbhurke.househunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rupeshbhurke.househunter.navigation.AppNavHost
import com.rupeshbhurke.househunter.ui.theme.HouseHunterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        /** Start the app on this flat's screen (used after sharing files into a flat). */
        const val EXTRA_ENTRY_ID = "entryId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseHunterTheme {
                AppNavHost(openEntryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it >= 0 })
            }
        }
    }
}
