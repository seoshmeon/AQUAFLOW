package com.zenhold.app.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeFont_keepsActionsVisible_andCompletesConsentFlow() {
        val completed = AtomicBoolean(false)

        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    OnboardingScreen(onComplete = { completed.set(true) })
                }
            }
        }

        composeRule.onNodeWithText("Продолжить").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Продолжить").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Начать спокойно").assertIsDisplayed()
        composeRule.onNodeWithText("Назад").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Я понимаю правила и прекращу подход при дискомфорте",
        ).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Начать спокойно").assertIsEnabled().performClick()

        composeRule.runOnIdle { assertTrue(completed.get()) }
    }
}
