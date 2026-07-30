package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorCoreTest {
    @Test
    fun digitReplacesInitialZero() {
        assertEdit("1", 1, ExpressionEditor.insertDigit("0", 0, '1', 50))
        assertEdit("7", 1, ExpressionEditor.insertDigit("0", 1, '7', 50))
        assertEdit("0", 1, ExpressionEditor.insertDigit("0", 1, '0', 50))
    }

    @Test
    fun digitReplacesZeroAfterOperator() {
        assertEdit("5 + 3", 5, ExpressionEditor.insertDigit("5 + 0", 4, '3', 50))
        assertEdit("5 + 3", 5, ExpressionEditor.insertDigit("5 + 0", 5, '3', 50))
        assertEdit("5 + 0", 5, ExpressionEditor.insertDigit("5 + 0", 5, '0', 50))
    }

    @Test
    fun decimalNumberKeepsExactlyOneLeadingZero() {
        assertEdit("0.5", 3, ExpressionEditor.insertDigit("0.", 2, '5', 50))
        assertEdit("0.5", 2, ExpressionEditor.insertDecimal("05", 1))
    }

    @Test
    fun tappingOperatorAgainReplacesItWithoutDamagingNumber() {
        assertEdit("12 × 3", 5, ExpressionEditor.insertOperator("12 + 3", 3, "×"))
        assertEdit("12 − 3", 5, ExpressionEditor.insertOperator("12 + 3", 4, "−"))
        assertEdit("12 ÷ 3", 5, ExpressionEditor.insertOperator("12 + 3", 5, "÷"))
    }

    @Test
    fun digitInsideOperatorIsIgnored() {
        assertEdit("12 + 3", 4, ExpressionEditor.insertDigit("12 + 3", 4, '9', 50))
    }

    @Test
    fun percentIsAttachedToEndOfCurrentNumber() {
        assertEdit("12%", 3, ExpressionEditor.insertPercent("12", 1))
        assertEdit("12%", 2, ExpressionEditor.insertPercent("12%", 2))
    }

    @Test
    fun backspaceRemovesWholeOperatorToken() {
        assertEdit("123", 2, ExpressionEditor.backspace("12 + 3", 5))
    }

    @Test
    fun engineUsesOperatorPrecedenceAndPreciseDecimals() {
        assertEquals("14", resultOf("2 + 3 × 4"))
        assertEquals("0.3", resultOf("0.1 + 0.2"))
    }

    @Test
    fun engineCalculatesCalculatorStylePercentages() {
        assertEquals("220", resultOf("200 + 10%"))
        assertEquals("20", resultOf("200 × 10%"))
    }

    @Test
    fun engineRejectsMalformedInputAndDivisionByZero() {
        assertThrows(IllegalArgumentException::class.java) {
            CalculatorEngine.evaluate("1 + + 2")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CalculatorEngine.evaluate("1 ÷ 0")
        }
    }

    private fun resultOf(expression: String): String {
        return CalculatorEngine.formatResult(CalculatorEngine.evaluate(expression))
    }

    private fun assertEdit(expression: String, cursor: Int, actual: EditResult) {
        assertEquals(expression, actual.expression)
        assertEquals(cursor, actual.cursor)
    }
}
