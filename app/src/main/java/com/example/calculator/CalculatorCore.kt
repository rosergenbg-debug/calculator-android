package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal data class EditResult(val expression: String, val cursor: Int)

internal object ExpressionEditor {
    private val operators = setOf("+", "−", "×", "÷")

    fun insertDigit(expression: String, cursor: Int, digit: Char, maxDigits: Int): EditResult {
        require(digit.isDigit())
        val safeCursor = cursor.coerceIn(0, expression.length)
        if (operatorRangeAt(expression, safeCursor) != null) return EditResult(expression, safeCursor)

        val bounds = numberBounds(expression, safeCursor)
        val token = expression.substring(bounds.first, bounds.second)
        val localCursor = (safeCursor - bounds.first).coerceIn(0, token.length)
        if (token.endsWith("%") && localCursor == token.length) return EditResult(expression, safeCursor)

        // A single zero is a placeholder for the current number. Replace it
        // regardless of whether Android placed the cursor before or after it.
        if (token == "0") {
            val replacement = digit.toString()
            val updated = expression.replaceRange(bounds.first, bounds.second, replacement)
            return EditResult(updated, bounds.first + replacement.length)
        }

        val candidate = token.substring(0, localCursor) + digit + token.substring(localCursor)
        if (candidate.count(Char::isDigit) > maxDigits) return EditResult(expression, safeCursor)

        val normalized = normalizeNumberToken(candidate)
        val updated = expression.replaceRange(bounds.first, bounds.second, normalized)
        val newCursor = if (candidate.startsWith("0") && candidate.length > 1) {
            bounds.first + normalized.removeSuffix("%").length
        } else {
            (safeCursor + 1).coerceAtMost(bounds.first + normalized.length)
        }
        return EditResult(updated.ifEmpty { "0" }, newCursor)
    }

    fun insertDecimal(expression: String, cursor: Int): EditResult {
        val safeCursor = cursor.coerceIn(0, expression.length)
        if (operatorRangeAt(expression, safeCursor) != null) return EditResult(expression, safeCursor)

        val bounds = numberBounds(expression, safeCursor)
        val token = expression.substring(bounds.first, bounds.second)
        val localCursor = (safeCursor - bounds.first).coerceIn(0, token.length)
        val percentIndex = token.indexOf('%')
        if (token.contains('.') || (percentIndex >= 0 && localCursor > percentIndex)) {
            return EditResult(expression, safeCursor)
        }

        val candidate = when {
            token.isEmpty() -> "0."
            localCursor == 0 -> "0.$token"
            else -> token.substring(0, localCursor) + "." + token.substring(localCursor)
        }
        val normalized = normalizeNumberToken(candidate)
        val updated = expression.replaceRange(bounds.first, bounds.second, normalized)
        val newCursor = when {
            token.isEmpty() || localCursor == 0 -> bounds.first + 2
            else -> safeCursor + 1
        }
        return EditResult(updated, newCursor.coerceAtMost(updated.length))
    }

    fun insertPercent(expression: String, cursor: Int): EditResult {
        val safeCursor = cursor.coerceIn(0, expression.length)
        if (operatorRangeAt(expression, safeCursor) != null) return EditResult(expression, safeCursor)

        val bounds = numberBounds(expression, safeCursor)
        val token = expression.substring(bounds.first, bounds.second)
        if (token.isEmpty() || token.contains('%')) return EditResult(expression, safeCursor)

        val completed = token.removeSuffix(".") + "%"
        val updated = expression.replaceRange(bounds.first, bounds.second, completed)
        return EditResult(updated, bounds.first + completed.length)
    }

    fun insertOperator(expression: String, cursor: Int, operator: String): EditResult {
        require(operator in operators)
        val safeCursor = cursor.coerceIn(0, expression.length)

        operatorRangeAt(expression, safeCursor)?.let { range ->
            val updated = expression.replaceRange(range.first, range.second, " $operator ")
            return EditResult(updated, range.first + 3)
        }

        val bounds = numberBounds(expression, safeCursor)
        previousOperatorRange(expression, bounds.first)?.takeIf { safeCursor == bounds.first }?.let { range ->
            val updated = expression.replaceRange(range.first, range.second, " $operator ")
            return EditResult(updated, range.first + 3)
        }
        nextOperatorRange(expression, bounds.second)?.takeIf { safeCursor == bounds.second }?.let { range ->
            val updated = expression.replaceRange(range.first, range.second, " $operator ")
            return EditResult(updated, range.first + 3)
        }

        var left = expression.substring(0, safeCursor).trimEnd()
        val right = expression.substring(safeCursor).trimStart()
        if (left.endsWith(".")) left = left.dropLast(1)
        if (left.isEmpty()) left = "0"
        if (right.startsWith("%")) return EditResult(expression, safeCursor)

        val updated = left + " $operator " + right
        return EditResult(updated, left.length + 3)
    }

    fun backspace(expression: String, cursor: Int): EditResult {
        val safeCursor = cursor.coerceIn(0, expression.length)
        if (safeCursor == 0) return EditResult(expression, safeCursor)

        operatorRangeBeforeOrAt(expression, safeCursor)?.let { range ->
            val updated = expression.removeRange(range.first, range.second).ifEmpty { "0" }
            return EditResult(updated, range.first.coerceAtMost(updated.length))
        }

        val raw = expression.removeRange(safeCursor - 1, safeCursor)
        if (raw.isEmpty()) return EditResult("0", 1)

        val probe = (safeCursor - 1).coerceAtMost(raw.length)
        val bounds = numberBounds(raw, probe)
        val token = raw.substring(bounds.first, bounds.second)
        val normalized = if (token.isEmpty()) token else normalizeNumberToken(token)
        val updated = raw.replaceRange(bounds.first, bounds.second, normalized).ifEmpty { "0" }
        val newCursor = (bounds.first + normalized.length).coerceAtMost(updated.length)
        return EditResult(updated, newCursor)
    }

    private fun normalizeNumberToken(token: String): String {
        val hasPercent = token.endsWith("%")
        val rawNumber = token.removeSuffix("%")
        if (rawNumber.isEmpty()) return token

        val parts = rawNumber.split(".", limit = 2)
        val integerPart = parts[0].dropWhile { it == '0' }.ifEmpty { "0" }
        val number = if (parts.size == 2) "$integerPart.${parts[1]}" else integerPart
        return number + if (hasPercent) "%" else ""
    }

    private fun numberBounds(expression: String, cursor: Int): Pair<Int, Int> {
        var start = cursor
        while (start > 0 && expression[start - 1] != ' ') start--
        var end = cursor
        while (end < expression.length && expression[end] != ' ') end++
        return start to end
    }

    private fun operatorRangeAt(expression: String, cursor: Int): Pair<Int, Int>? {
        return operatorRanges(expression).firstOrNull { cursor > it.first && cursor < it.second }
    }

    private fun operatorRangeBeforeOrAt(expression: String, cursor: Int): Pair<Int, Int>? {
        return operatorRanges(expression).firstOrNull { cursor > it.first && cursor <= it.second }
    }

    private fun previousOperatorRange(expression: String, numberStart: Int): Pair<Int, Int>? {
        return operatorRanges(expression).lastOrNull { it.second == numberStart }
    }

    private fun nextOperatorRange(expression: String, numberEnd: Int): Pair<Int, Int>? {
        return operatorRanges(expression).firstOrNull { it.first == numberEnd }
    }

    private fun operatorRanges(expression: String): List<Pair<Int, Int>> {
        if (expression.length < 3) return emptyList()
        return buildList {
            for (index in 0..expression.length - 3) {
                if (
                    expression[index] == ' ' &&
                    expression[index + 1].toString() in operators &&
                    expression[index + 2] == ' '
                ) {
                    add(index to index + 3)
                }
            }
        }
    }
}

internal object CalculatorEngine {
    private val mathContext = MathContext(30, RoundingMode.HALF_UP)
    private val operators = setOf("+", "−", "×", "÷")

    fun evaluate(expression: String): BigDecimal {
        val tokens = expression.split(" ").filter { it.isNotBlank() }
        require(tokens.isNotEmpty() && tokens.size % 2 == 1) { "Incomplete expression" }

        val values = mutableListOf<NumberToken>()
        val operations = mutableListOf<String>()
        tokens.forEachIndexed { index, token ->
            if (index % 2 == 0) {
                values.add(parseNumberToken(token))
            } else {
                require(token in operators) { "Unknown operator" }
                operations.add(token)
            }
        }

        var index = 0
        while (index < operations.size) {
            val operator = operations[index]
            if (operator == "×" || operator == "÷") {
                val left = values[index].asStandaloneValue()
                val right = values[index + 1].asStandaloneValue()
                values[index] = NumberToken(applyOperator(left, right, operator), false)
                values.removeAt(index + 1)
                operations.removeAt(index)
            } else {
                index++
            }
        }

        var result = values.first().asStandaloneValue()
        operations.forEachIndexed { operationIndex, operator ->
            val token = values[operationIndex + 1]
            val right = if (token.isPercent) {
                result.multiply(token.percentDecimal(), mathContext)
            } else {
                token.value
            }
            result = applyOperator(result, right, operator)
        }
        return result
    }

    fun formatResult(value: BigDecimal): String {
        val normalized = value.stripTrailingZeros()
        return if (normalized.compareTo(BigDecimal.ZERO) == 0) "0" else normalized.toPlainString()
    }

    fun normalizeForEvaluation(expression: String): String {
        var clean = expression.trim()
        if (clean.lastOrNull()?.toString() in operators) clean = clean.dropLast(1).trim()
        if (clean.endsWith(".")) clean = clean.dropLast(1)
        return clean
    }

    fun hasMathAction(expression: String): Boolean {
        return operators.any { expression.contains(" $it ") } || expression.contains("%")
    }

    private fun parseNumberToken(token: String): NumberToken {
        require(token.count { it == '%' } <= 1 && (!token.contains('%') || token.endsWith('%'))) {
            "Invalid percent"
        }
        val isPercent = token.endsWith("%")
        val clean = token.removeSuffix("%").replace(',', '.')
        require(clean.isNotBlank() && clean != ".") { "Invalid number" }
        return NumberToken(BigDecimal(clean), isPercent)
    }

    private fun applyOperator(left: BigDecimal, right: BigDecimal, operator: String): BigDecimal {
        return when (operator) {
            "+" -> left.add(right, mathContext)
            "−" -> left.subtract(right, mathContext)
            "×" -> left.multiply(right, mathContext)
            "÷" -> {
                require(right.compareTo(BigDecimal.ZERO) != 0) { "Division by zero" }
                left.divide(right, mathContext)
            }
            else -> error("Unknown operator")
        }
    }

    private data class NumberToken(val value: BigDecimal, val isPercent: Boolean) {
        fun percentDecimal(): BigDecimal = value.divide(BigDecimal("100"), mathContext)
        fun asStandaloneValue(): BigDecimal = if (isPercent) percentDecimal() else value
    }
}
