package com.movtery.zalithlauncher.ui.screens.main

import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Navigation wiring of the RKB top bar (pure JVM, no Android runtime needed). */
class RkbNavigationTest {
    @Test
    fun pillShowsMainDestinationsInReferenceOrder() {
        val pill = RkbDestination.entries.filter { it.inPill }
        assertEquals(
            listOf(
                RkbDestination.Home, RkbDestination.Instances, RkbDestination.Download,
                RkbDestination.Mods, RkbDestination.Control
            ),
            pill
        )
    }

    @Test
    fun accountAboutAndSettingsAreReachedFromTheRightCluster() {
        listOf(RkbDestination.Account, RkbDestination.About, RkbDestination.Setting)
            .forEach { assertFalse("$it must not be in the pill", it.inPill) }
    }

    @Test
    fun startupScreenSelectsHome() {
        assertEquals(RkbDestination.Home, rkbSelectedDestination(null))
        assertEquals(RkbDestination.Home, rkbSelectedDestination(NormalNavKey.LauncherMain))
    }

    @Test
    fun topLevelScreensMapToTheirPillItem() {
        // Keys whose titles need Android resources (VersionsManager, AccountManager) are covered on-device.
        assertEquals(RkbDestination.Mods, rkbSelectedDestination(NormalNavKey.ModSettings))
    }

    @Test
    fun everyDestinationHasALabel() {
        assertTrue(RkbDestination.entries.all { it.label.isNotBlank() })
    }
}
