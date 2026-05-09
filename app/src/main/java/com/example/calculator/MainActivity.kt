package com.example.calculator

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: EditText
    private lateinit var tvExpression: TextView
    private lateinit var tvPreview: TextView

    private var fullExpression = "0"
    private var isResultShown = false

    private val maxDigits = 50
    private val mathContext = MathContext(30, RoundingMode.HALF_UP)
    private val operators = setOf("+", "−", "×", "÷")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        tvPreview = findViewById(R.id.tvPreview)

        tvDisplay.showSoftInputOnFocus = false

        tvExpression.text = ""
        tvPreview.text = ""
        renderDisplay()

        setupButtons()
    }

    private fun setupButtons() {
        listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        ).forEachIndexed { digit, id ->
            findViewById<View>(id).setOnClickListener { insertDigit(digit.toString()) }
        }

        findViewById<View>(R.id.btnDecimal).setOnClickListener { insertDecimal() }
        findViewById<View>(R.id.btnPlus).setOnClickListener { insertOperator("+") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { insertOperator("−") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { insertOperator("×") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { insertOperator("÷") }

        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.btnBackspace).setOnClickListener { onBackspace() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { insertPercent() }
    }

    private fun getCursorPos(): Int {
        val sel = tvDisplay.selectionStart
        return if (sel >= 0) sel.coerceAtMost(fullExpression.length) else fullExpression.length
    }

    private fun insertDigit(digit: String) {
        clearErrorIfNeeded()

        if (isResultShown) {
            fullExpression = digit
            isResultShown = false
            tvExpression.text = ""
            renderDisplay(1)
            updatePreview()
            return
        }

        replaceSelection { before, after, pos ->
            if (before.endsWith("%")) return

            val number = currentNumber(before, after)
            if (number.replace(".", "").replace("%", "").length >= maxDigits) return

            val newBefore = if (fullExpression == "0" && before == "0" && after.isEmpty()) digit else before + digit
            fullExpression = newBefore + after
            renderDisplay(if (newBefore == digit && before == "0") 1 else pos + 1)
            updatePreview()
        }
    }

    private fun insertDecimal() {
        clearErrorIfNeeded()

        if (isResultShown) {
            fullExpression = "0."
            isResultShown = false
            tvExpression.text = ""
            renderDisplay(2)
            updatePreview()
            return
        }

        replaceSelection { before, after, pos ->
            if (before.endsWith("%")) return

            val number = currentNumber(before, after)
            if (number.contains(".")) return

            if (number.isEmpty()) {
                fullExpression = before + "0." + after
                renderDisplay(pos + 2)
            } else {
                fullExpression = before + "." + after
                renderDisplay(pos + 1)
            }
            updatePreview()
        }
    }

    private fun insertOperator(operator: String) {
        clearErrorIfNeeded()

        if (isResultShown) {
            isResultShown = false
            tvExpression.text = ""
        }

        replaceSelection { before, after, pos ->
            var newBefore = before
            var newPos = pos

            if (newBefore.endsWith(" ")) {
                newBefore = newBefore.dropLast(3)
                newPos -= 3
            }
            if (newBefore.endsWith(".")) {
                newBefore = newBefore.dropLast(1)
                newPos -= 1
            }
            if (newBefore.isEmpty()) {
                newBefore = "0"
                newPos = 1
            }

            fullExpression = newBefore + " $operator " + after.trimStart()
            renderDisplay(newPos + 3)
            updatePreview()
        }
    }

    private fun insertPercent() {
        clearErrorIfNeeded()
        if (isResultShown) {
            isResultShown = false
            tvExpression.text = ""
        }

        replaceSelection { before, after, pos ->
            val number = currentNumber(before, after)
            if (number.isEmpty() || number.endsWith("%")) return

            val newBefore = if (before.endsWith(".")) before.dropLast(1) else before
            val cursorShift = if (before.endsWith(".")) 0 else 1
            fullExpression = newBefore + "%" + after
            renderDisplay(pos + cursorShift)
            updatePreview()
        }
    }

    private fun onBackspace() {
        clearErrorIfNeeded()
        if (isResultShown) {
            onClear()
            return
        }

        val pos = getCursorPos()
        if (pos == 0) return

        val charsToDelete = if (pos >= 3 && fullExpression.substring(pos - 3, pos).isOperatorToken()) 3 else 1
        val before = fullExpression.substring(0, pos - charsToDelete)
        val after = fullExpression.substring(pos)

        fullExpression = (before + after).ifEmpty { "0" }
        renderDisplay((pos - charsToDelete).coerceAtLeast(0))
        updatePreview()
    }

    private fun onEquals() {
        if (fullExpression == "0" || isResultShown) return

        val cleanExpr = fullExpression.normalizedForEvaluation()
        if (cleanExpr.isBlank()) return

        try {
            val result = evaluate(cleanExpr)
            tvExpression.text = "${cleanExpr.replace('.', ',')} ="
            fullExpression = formatResult(result)
            renderDisplay(fullExpression.length)
            tvPreview.text = ""
            isResultShown = true
        } catch (e: Exception) {
            showError()
        }
    }

    private fun onClear() {
        fullExpression = "0"
        tvExpression.text = ""
        tvPreview.text = ""
        renderDisplay(1)
        isResultShown = false
    }

    private fun updatePreview() {
        if (isResultShown || fullExpression == "0" || !fullExpression.hasMathAction()) {
            tvPreview.text = ""
            return
        }

        val cleanExpr = fullExpression.normalizedForEvaluation()
        if (cleanExpr.isBlank() || cleanExpr == "0") {
            tvPreview.text = ""
            return
        }

        try {
            tvPreview.text = "= " + formatResult(evaluate(cleanExpr)).replace('.', ',')
        } catch (e: Exception) {
            tvPreview.text = ""
        }
    }

    private fun renderDisplay(newCursorPos: Int? = null) {
        val currentCursor = getCursorPos()
        val displayStr = fullExpression.replace('.', ',')

        val textSize = when {
            displayStr.length <= 8 -> 64f
            displayStr.length <= 15 -> 48f
            displayStr.length <= 25 -> 36f
            else -> 28f
        }
        tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        tvDisplay.setText(displayStr)

        val finalPos = (newCursorPos ?: currentCursor).coerceIn(0, displayStr.length)
        tvDisplay.setSelection(finalPos)
    }

    private fun evaluate(expr: String): BigDecimal {
        val tokens = expr.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return BigDecimal.ZERO
        if (tokens.last() in operators) throw IllegalArgumentException("Expression ends with operator")

        val values = mutableListOf<NumberToken>()
        val ops = mutableListOf<String>()

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (index % 2 == 0) {
                values.add(parseNumberToken(token))
            } else {
                if (token !in operators) throw IllegalArgumentException("Unknown operator")
                ops.add(token)
            }
            index++
        }

        var i = 0
        while (i < ops.size) {
            val op = ops[i]
            if (op == "×" || op == "÷") {
                val left = values[i].asStandaloneValue()
                val right = values[i + 1].asStandaloneValue()
                values[i] = NumberToken(applyOperator(left, right, op), false)
                values.removeAt(i + 1)
                ops.removeAt(i)
            } else {
                i++
            }
        }

        var result = values.first().asStandaloneValue()
        ops.forEachIndexed { opIndex, op ->
            val token = values[opIndex + 1]
            val right = if (token.isPercent) {
                result.multiply(token.percentDecimal(), mathContext)
            } else {
                token.value
            }
            result = applyOperator(result, right, op)
        }
        return result
    }

    private fun parseNumberToken(token: String): NumberToken {
        if (token.count { it == '%' } > 1) throw NumberFormatException("Too many percent signs")

        val isPercent = token.endsWith("%")
        val clean = token.removeSuffix("%").replace(',', '.')
        if (clean.isBlank() || clean == ".") throw NumberFormatException("Invalid number")

        return NumberToken(BigDecimal(clean), isPercent)
    }

    private fun applyOperator(left: BigDecimal, right: BigDecimal, operator: String): BigDecimal {
        return when (operator) {
            "+" -> left.add(right, mathContext)
            "−" -> left.subtract(right, mathContext)
            "×" -> left.multiply(right, mathContext)
            "÷" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                left.divide(right, mathContext)
            }
            else -> throw IllegalArgumentException("Unknown operator")
        }
    }

    private inline fun replaceSelection(block: (before: String, after: String, pos: Int) -> Unit) {
        val pos = getCursorPos()
        block(fullExpression.substring(0, pos), fullExpression.substring(pos), pos)
    }

    private fun currentNumber(before: String, after: String): String {
        val left = before.substringAfterLast(" ")
        val right = after.substringBefore(" ")
        return left + right
    }

    private fun clearErrorIfNeeded() {
        if (tvDisplay.text.toString() == "Fehler") onClear()
    }

    private fun showError() {
        tvDisplay.setText("Fehler")
        fullExpression = "0"
        tvPreview.text = ""
        isResultShown = true
    }

    private fun formatResult(value: BigDecimal): String {
        val normalized = value.stripTrailingZeros()
        return if (normalized.compareTo(BigDecimal.ZERO) == 0) "0" else normalized.toPlainString()
    }

    private fun String.isOperatorToken(): Boolean {
        return this.length == 3 && this.first() == ' ' && this.last() == ' ' && this.trim() in operators
    }

    private fun String.hasMathAction(): Boolean {
        return contains(" + ") || contains(" − ") || contains(" × ") || contains(" ÷ ") || contains("%")
    }

    private fun String.normalizedForEvaluation(): String {
        var clean = trim()
        if (clean.endsWith("+") || clean.endsWith("−") || clean.endsWith("×") || clean.endsWith("÷")) {
            clean = clean.dropLast(1).trim()
        }
        if (clean.endsWith(".")) clean = clean.dropLast(1)
        return clean
    }

    private data class NumberToken(val value: BigDecimal, val isPercent: Boolean) {
        fun percentDecimal(): BigDecimal = value.divide(BigDecimal("100"), MathContext(30, RoundingMode.HALF_UP))

        fun asStandaloneValue(): BigDecimal {
            return if (isPercent) percentDecimal() else value
        }
    }
}
