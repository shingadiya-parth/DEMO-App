package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.PlayRewardsTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SplashScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreenShot() {
        composeTestRule.setContent {
            PlayRewardsTheme {
                SplashScreen()
            }
        }
        composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("splash_screen"))
            .captureRoboImage("src/test/screenshots/splash_screen.png")
    }
}
