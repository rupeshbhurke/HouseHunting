package com.rupeshbhurke.househunter

import org.junit.Assert.assertEquals
import org.junit.Test

/** Keeps the unit-test pipeline exercised until real tests arrive in M1. */
class SanityTest {
    @Test
    fun buildConfig_hasExpectedApplicationId() {
        assertEquals("com.rupeshbhurke.househunter", BuildConfig.APPLICATION_ID)
    }
}
