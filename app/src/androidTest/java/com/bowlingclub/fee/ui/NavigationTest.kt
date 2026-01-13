package com.bowlingclub.fee.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bowlingclub.fee.ui.navigation.BottomNavItem
import com.bowlingclub.fee.ui.theme.BowlingClubFeeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavItems_haveCorrectLabels() {
        // Verify bottom nav items have correct Korean labels
        val items = BottomNavItem.entries

        assert(items.any { it.label == "홈" })
        assert(items.any { it.label == "회원" })
        assert(items.any { it.label == "회비" })
        assert(items.any { it.label == "장부" })
        assert(items.any { it.label == "점수" })
    }

    @Test
    fun bottomNavItems_haveCorrectRoutes() {
        val items = BottomNavItem.entries

        assert(items.any { it.route == "home" })
        assert(items.any { it.route == "members" })
        assert(items.any { it.route == "payments" })
        assert(items.any { it.route == "accounts" })
        assert(items.any { it.route == "scores" })
    }
}
