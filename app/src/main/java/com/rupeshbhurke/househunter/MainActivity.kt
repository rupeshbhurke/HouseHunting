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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseHunterTheme {
                AppNavHost()
            }
        }
    }
}
