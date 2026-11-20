package com.rupeshbhurke.househunter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.rupeshbhurke.househunter.ui.theme.HouseHunterTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base for screenshot tests: renders a composable on the JVM and writes a PNG.
 *
 * Goldens live in `app/src/test/screenshots/` and are committed. Re-record them
 * with `gradlew recordRoborazziDebug` whenever a change is meant to alter the
 * UI, and commit the new PNGs with it; `factory/checks.sh` runs
 * `verifyRoborazziDebug`, which fails on any unrecorded visual change.
 *
 * Only stateless composables can be captured. Anything that calls
 * `hiltViewModel()` has to be split into a stateful wrapper and a stateless
 * `...Content(state, callbacks)` before it can be screenshotted.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
abstract class SnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * Renders [content] inside the app's real theme and captures it.
     *
     * `dynamicColor = false` keeps goldens deterministic: dynamic colour derives
     * the palette from the host device, so it would differ per machine.
     */
    protected fun snapshot(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            HouseHunterTheme(darkTheme = dark, dynamicColor = false) {
                Surface {
                    Box(Modifier.padding(16.dp)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
