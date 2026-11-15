package com.rupeshbhurke.househunter

import android.app.Application
import com.rupeshbhurke.househunter.core.data.repo.CollectionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HouseHunterApp : Application() {
    @Inject lateinit var collections: CollectionRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { collections.syncBuiltInTemplates() }
    }
}
