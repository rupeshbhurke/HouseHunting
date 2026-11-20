package com.rupeshbhurke.househunter.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rupeshbhurke.househunter.ui.theme.HouseHunterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What TalkBack and touch see of [RatingStars]: semantics, target sizes and clicks.
 * Nothing here is visual, so it captures no images.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RatingStarsSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) =
        compose.setContent { HouseHunterTheme(dynamicColor = false) { content() } }

    @Test
    fun readOnly_rated_isOneAnnouncement() {
        show { RatingStars(rating = 4) }

        compose.onNodeWithContentDescription("Rated 4 of 5").assertExists()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun readOnly_unrated_saysNotRated_andIsNotClickable() {
        show { RatingStars(rating = null) }

        compose.onNodeWithContentDescription("Not rated").assertExists()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun interactive_eachStarHas48dpTarget() {
        show { RatingStars(rating = 2, size = 32.dp, onRate = {}) }

        for (star in 1..5) {
            compose.onNodeWithContentDescription("Rate $star of 5")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .assertHasClickAction()
        }
        compose.onAllNodesWithContentDescription("Rated", substring = true).assertCountEquals(0)
    }

    @Test
    fun interactive_filledStarsAreSelected() {
        show { RatingStars(rating = 3, size = 32.dp, onRate = {}) }

        for (star in 1..3) compose.onNodeWithContentDescription("Rate $star of 5").assertIsSelected()
        for (star in 4..5) compose.onNodeWithContentDescription("Rate $star of 5").assertIsNotSelected()
    }

    @Test
    fun interactive_tapSetsRating_andTappingCurrentClearsIt() {
        val rated = mutableListOf<Int?>()
        show { RatingStars(rating = 4, size = 32.dp, onRate = { rated += it }) }

        compose.onNodeWithContentDescription("Rate 2 of 5").performClick()
        compose.onNodeWithContentDescription("Rate 4 of 5").performClick()

        assertEquals(listOf(2, null), rated)
    }
}
