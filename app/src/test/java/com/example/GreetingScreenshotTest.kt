package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Product
import com.example.ui.components.ProductGridCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun product_card_screenshot() {
    val expiryCal = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 10)
    }
    val sampleProduct = Product(
      id = 1L,
      name = "Iogurte Natural 170g",
      category = "Laticínios",
      quantity = 24,
      unit = "un",
      expiryDate = expiryCal.timeInMillis,
      batchCode = "LOT-1024",
      location = "Geladeira 02",
      regularPrice = 4.50
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ProductGridCard(
          product = sampleProduct,
          isSelected = false,
          onSelectCard = {},
          onCardClick = {},
          onRequestMarkdown = {},
          onConfirmMarkdownAcceptance = { _, _ -> },
          onConfirmRemoval = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
