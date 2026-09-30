package com.example

import com.example.util.Gender
import com.example.util.NameGenerator
import com.example.util.TotpHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNameGeneratorForBangladesh() {
    val maleName = NameGenerator.generateName("BD", Gender.MALE)
    assertNotNull(maleName)
    assertTrue(maleName.isNotBlank())
    assertTrue(maleName.contains(" "))

    val femaleName = NameGenerator.generateName("BD", Gender.FEMALE)
    assertNotNull(femaleName)
    assertTrue(femaleName.isNotBlank())
  }

  @Test
  fun testTotpGeneration() {
    // Standard RFC test key
    val secretKey = "JBSWY3DPEHPK3PXP"
    val result = TotpHelper.generateTotp(secretKey, 1600000000000L)
    assertNotNull(result)
    assertEquals(6, result!!.code.length)
    assertTrue(result.remainingSeconds in 1..30)
  }
}

