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
    private val MAX_DIGITS = 50
    private val mathContext = MathContext(30, RoundingMode.HALF_UP)

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
        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, 
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        ids.forEachIndexed { i, id -> findViewById<View>(id).setOnClickListener { insertText(i.toString()) } }
        
        findViewById<View>(R.id.btnDecimal).setOnClickListener { insertText(".") }
        findViewById<View>(R.id.btnPlus).setOnClickListener { insertText(" + ") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { insertText(" − ") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { insertText(" × ") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { insertText(" ÷ ") }
        
        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.btnBackspace).setOnClickListener { onBackspace() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { insertText("%") }
    }

    private fun getCursorPos(): Int {
        val sel = tvDisplay.selectionStart
        return if (sel >= 0) sel else fullExpression.length
    }

    private fun checkErrorState() {
        if (tvDisplay.text.toString() == "Fehler") onClear()
    }

    private fun insertText(text: String) {
        checkErrorState()
        
        if (isResultShown) {
            fullExpression = if (text.matches(Regex(".*[0-9].*"))) text else fullExpression + text
            isResultShown = false
            renderDisplay(fullExpression.length)
            updatePreview()
            return
        }

        var pos = getCursorPos()
        
        if (fullExpression == "0" && text.matches(Regex("[0-9]"))) {
            fullExpression = text
            renderDisplay(text.length)
            updatePreview()
            return
        }

        var before = fullExpression.substring(0, pos)
        val after = fullExpression.substring(pos)
        
        if (text.contains(" ")) { 
            if (before.endsWith(" ")) {
                before = before.dropLast(3)
                pos -= 3
            }
            if (before.endsWith(".")) {
                before = before.dropLast(1)
                pos -= 1
            }
            if (before.isEmpty()) {
                before = "0"
                pos = 1
            }
        }
        
        if (text == ".") {
            val lastToken = before.split(" ").last()
            if (lastToken.contains(".")) return
            if (lastToken.isEmpty()) {
                fullExpression = before + "0." + after
                renderDisplay(pos + 2)
                updatePreview()
                return
            }
        }

        if (text == "%") {
            val lastToken = before.split(" ").last()
            if (lastToken.isEmpty() || lastToken.contains("%")) return
            if (before.endsWith(".")) {
                before = before.dropLast(1)
                pos -= 1
            }
        }
        
        if (text.matches(Regex("[0-9]"))) {
            val lastTokenBefore = before.split(" ").last()
            val firstTokenAfter = after.split(" ").firstOrNull() ?: ""
            val fullCurrentNumber = (lastTokenBefore + firstTokenAfter).replace(".", "").replace("%", "")
            if (fullCurrentNumber.length >= MAX_DIGITS) return
        }

        fullExpression = before + text + after
        renderDisplay(pos + text.length)
        updatePreview()
    }

    private fun onBackspace() {
        checkErrorState()
        if (isResultShown) {
            onClear()
            return
        }
        
        val pos = getCursorPos()
        if (pos == 0) return 

        var charsToDelete = 1
        if (pos >= 3) {
            val last3 = fullExpression.substring(pos - 3, pos)
            if (last3 == " + " || last3 == " − " || last3 == " × " || last3 == " ÷ ") {
                charsToDelete = 3
            }
        }

        val before = fullExpression.substring(0, pos - charsToDelete)
        val after = fullExpression.substring(pos)
        
        fullExpression = before + after
        if (fullExpression.isEmpty()) fullExpression = "0"
        
        renderDisplay(pos - charsToDelete)
        updatePreview()
    }

    // ТЕНЕВОЙ ВЫЧИСЛИТЕЛЬ: считает пример в реальном времени
    private fun updatePreview() {
        if (isResultShown || fullExpression == "0") {
            tvPreview.text = ""
            return
        }

        // Показываем предпросмотр, только если в строке есть математическое действие
        val hasOperator = fullExpression.contains(" + ") || fullExpression.contains(" − ") || 
                          fullExpression.contains(" × ") || fullExpression.contains(" ÷ ") || 
                          fullExpression.contains("%")
                          
        if (!hasOperator) {
            tvPreview.text = ""
            return
        }

        var cleanExpr = if (fullExpression.endsWith(" ")) fullExpression.dropLast(3) else fullExpression
        if (cleanExpr.endsWith(".")) cleanExpr = cleanExpr.dropLast(1)
        
        try {
            val result = evaluate(cleanExpr)
            // Добавляем знак равно и меняем точки на запятые
            tvPreview.text = "= " + formatResult(result).replace('.', ',')
        } catch (e: Exception) {
            // Если пример некорректный (например, деление на ноль в процессе), прячем предпросмотр
            tvPreview.text = ""
        }
    }

    private fun renderDisplay(newCursorPos: Int? = null) {
        val currentCursor = getCursorPos()
        val displayStr = fullExpression.replace('.', ',')
        
        val len = displayStr.length
        when {
            len <= 8 -> tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64f)
            len <= 15 -> tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            len <= 25 -> tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
            else -> tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        }
        
        tvDisplay.setText(displayStr)
        
        val finalPos = newCursorPos ?: currentCursor
        if (finalPos in 0..displayStr.length) {
            tvDisplay.setSelection(finalPos)
        } else {
            tvDisplay.setSelection(displayStr.length)
        }
    }

    private fun onEquals() {
        if (fullExpression.isEmpty() || isResultShown || fullExpression == "0") return
        var cleanExpr = if (fullExpression.endsWith(" ")) fullExpression.dropLast(3) else fullExpression
        if (cleanExpr.endsWith(".")) cleanExpr = cleanExpr.dropLast(1)
        
        try {
            val result = evaluate(cleanExpr)
            tvExpression.text = "${cleanExpr.replace('.', ',')} =" 
            fullExpression = formatResult(result)
            renderDisplay(fullExpression.length) 
            tvPreview.text = "" // Убираем оранжевый предпросмотр, так как результат уже на основном экране
            isResultShown = true
        } catch (e: Exception) {
            tvDisplay.setText("Fehler")
            fullExpression = "0"
            tvPreview.text = ""
            isResultShown = true
        }
    }

    private fun onClear() {
        fullExpression = "0"
        tvExpression.text = ""
        tvPreview.text = ""
        renderDisplay(1)
        isResultShown = false
    }

    private fun evaluate(expr: String): BigDecimal {
        val tokens = expr.trim().split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return BigDecimal.ZERO
        
        var result = parseFirstToken(tokens[0])
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextStr = tokens.getOrNull(i + 1) ?: "0"
            val isPercent = nextStr.endsWith("%")
            val cleanNextStr = nextStr.replace("%", "").replace(',', '.')
            var nextVal = try { BigDecimal(cleanNextStr) } catch(e: Exception) { BigDecimal.ZERO }
            
            if (isPercent) {
                val percentDecimal = nextVal.divide(BigDecimal("100"), mathContext)
                nextVal = if (op == "+" || op == "−" || op == "-") result.multiply(percentDecimal, mathContext) else percentDecimal
            }
            
            result = when (op) {
                "+" -> result.add(nextVal, mathContext)
                "−", "-" -> result.subtract(nextVal, mathContext)
                "×", "*" -> result.multiply(nextVal, mathContext)
                "÷", "/" -> {
                    if (nextVal.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Div by zero")
                    result.divide(nextVal, mathContext)
                }
                else -> result
            }
            i += 2
        }
        return result
    }

    private fun parseFirstToken(token: String): BigDecimal {
        val isPercent = token.endsWith("%")
        val clean = token.replace("%", "").replace(',', '.')
        val v = try { BigDecimal(clean) } catch(e: Exception) { BigDecimal.ZERO }
        return if (isPercent) v.divide(BigDecimal("100"), mathContext) else v
    }

    private fun formatResult(v: BigDecimal): String {
        var resultStr = v.stripTrailingZeros().toPlainString()
        if (resultStr == "0.0") resultStr = "0"
        return resultStr
    }
}
