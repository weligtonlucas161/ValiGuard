package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExpiryStatus
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Validade Supermercado", appName)
  }

  @Test
  fun `test 15 days markdown alert eligibility`() {
    val now = System.currentTimeMillis()
    val cal12Days = Calendar.getInstance().apply {
      timeInMillis = now
      add(Calendar.DAY_OF_YEAR, 12)
    }
    val product = Product(
      name = "Iogurte Natural",
      category = "Laticínios",
      quantity = 10,
      expiryDate = cal12Days.timeInMillis,
      regularPrice = 10.0,
      markdownDiscountPercent = 30
    )

    assertTrue(product.isMarkdownEligible(now))
    assertEquals(ExpiryStatus.MARKDOWN_REBAIXA, product.getExpiryStatus(now))
    assertEquals(7.0, product.calculateSuggestedMarkdownPrice(), 0.01)
  }

  @Test
  fun `test default login user001 and matricula 123456789`() {
    val defaultUser = "user001"
    val defaultMatricula = "123456789"
    val authUser = com.example.ui.viewmodel.AuthUser(defaultUser, defaultMatricula)

    assertEquals("user001", authUser.username)
    assertEquals("123456789", authUser.registrationNumber)
    assertTrue(authUser.loginTimeMillis > 0)
  }
}
