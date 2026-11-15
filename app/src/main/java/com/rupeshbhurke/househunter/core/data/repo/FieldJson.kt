package com.rupeshbhurke.househunter.core.data.repo

import kotlinx.serialization.json.Json

internal val FieldJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
