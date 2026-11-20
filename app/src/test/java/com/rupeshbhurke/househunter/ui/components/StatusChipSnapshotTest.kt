package com.rupeshbhurke.househunter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.rupeshbhurke.househunter.core.data.model.EntryStatus
import com.rupeshbhurke.househunter.ui.theme.HouseHunterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Spike: proves Compose renders to a PNG on the JVM with no emulator.
 *
 * Record a golden with `gradlew recordRoborazziDebug`, check a change against it
 * with `gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class StatusChipSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun statusChip_everyStatus() {
        compose.setContent {
            // dynamicColor = false keeps the golden deterministic. Dynamic colour
            // derives the palette from the host, so it would differ per machine.
            HouseHunterTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EntryStatus.entries.forEach { StatusChip(it) }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
