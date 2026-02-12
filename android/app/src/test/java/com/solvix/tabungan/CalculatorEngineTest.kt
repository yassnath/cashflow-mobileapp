package com.solvix.tabungan

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {

  private fun runKeys(vararg keys: String): String {
    var display = "0"
    keys.forEach { key ->
      display = calculate(display, key)
    }
    return display
  }

  @Test
  fun calculatesBasicAddition() {
    val result = runKeys("2", "+", "3", "=")
    assertEquals("5", result)
  }

  @Test
  fun respectsOperatorPrecedence() {
    val result = runKeys("2", "+", "3", "×", "4", "=")
    assertEquals("14", result)
  }

  @Test
  fun supportsParentheses() {
    val result = runKeys("(", "2", "+", "3", ")", "×", "4", "=")
    assertEquals("20", result)
  }

  @Test
  fun supportsImplicitMultiplicationWithParentheses() {
    val result = runKeys("2", "(", "3", "+", "4", ")", "=")
    assertEquals("14", result)
  }

  @Test
  fun supportsScientificFunctionSin() {
    val result = runKeys("sin", "3", "0", ")", "=")
    assertEquals("0.5", result)
  }

  @Test
  fun supportsSquareRoot() {
    val result = runKeys("√", "9", ")", "=")
    assertEquals("3", result)
  }

  @Test
  fun supportsPercent() {
    val result = runKeys("2", "0", "0", "×", "1", "0", "%", "=")
    assertEquals("20", result)
  }

  @Test
  fun returnsErrorForInvalidResult() {
    val result = runKeys("1", "÷", "0", "=")
    assertEquals("Error", result)
  }
}

