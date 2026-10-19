package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Port of Tests/AppModuleTests.swift. */
class AppModuleTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    @Test
    fun testSavedModuleValuesKeepTheirExistingMeaning() {
        val existingValues = listOf(
            0 to AppModule.Schedule, 1 to AppModule.Ledger, 2 to AppModule.Diary,
            3 to AppModule.Settings, 4 to AppModule.Agenda
        )
        for ((rawValue, expectedModule) in existingValues) {
            assertEquals("raw value $rawValue", expectedModule, AppModule.restored(rawValue))
        }
        assertEquals("selectedModule", AppModule.storageKey)
    }

    @Test
    fun testFirstLaunchAndInvalidSavedValuesOpenAgenda() {
        // Swift's Int is 64-bit; AppModule.restored takes a Kotlin Int, so Int.max maps to Int.MAX_VALUE.
        for (rawValue in listOf<Int?>(null, -1, 5, 99, Int.MAX_VALUE)) {
            assertEquals("raw value $rawValue", AppModule.Agenda, AppModule.restored(rawValue))
        }
    }
}
