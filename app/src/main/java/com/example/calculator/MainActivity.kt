package com.example.calculator

import android.os.Bundle
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
    
    private var fullExpression = "0"
    private var isResultShown = false
    private val mathContext = MathContext(30, RoundingMode.HALF_UP)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        
        // Гарантированно отключаем системную клавиатуру
        tvDisplay.showSoftInputOnFocus = false
        
        tvExpression.text = ""
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

    // Универсальная функция вставки в место курсора
    private fun insertText(text: String) {
        checkErrorState()
        
        if (isResultShown) {
            fullExpression = if (text.contains(Regex("[0-9]"))) text else fullExpression + text
            isResultShown = false
            renderDisplay(fullExpression.length)
            return
        }

        var pos = getCursorPos()
        
        // Если на экране только "0" и мы вводим цифру, заменяем этот ноль
        if (fullExpression == "0" && text.matches(Regex("[0-9]"))) {
            fullExpression = text
            renderDisplay(text.length)
            return
        }

        // Разрезаем строку по курсору и вставляем новый кусок
        val before = fullExpression.substring(0, pos)
        val after = fullExpression.substring(pos)
        
        fullExpression = before + text + after
        renderDisplay(pos + text.length)
    }

    private fun onBackspace() {
        checkErrorState()
        if (isResultShown) {
            onClear()
            return
        }
        
        val pos = getCursorPos()
        if (pos == 0) return // Нечего удалять, курсор в самом начале

        var charsToDelete = 1
        
        // Умное удаление: если перед курсором стоит оператор с пробелами, стираем его целиком (3 символа)
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
    }

    private fun renderDisplay(newCursorPos: Int? = null) {
        val currentCursor = getCursorPos()
        val displayStr = fullExpression.replace('.', ',')
        
        tvDisplay.setText(displayStr)
        
        val finalPos = newCursorPos ?: currentCursor
        // Защита от выхода курсора за пределы строки
        if (finalPos in 0..displayStr.length) {
            tvDisplay.setSelection(finalPos)
        } else {
            tvDisplay.setSelection(displayStr.length)
        }
    }

    private fun onEquals() {
        if (fullExpression.isEmpty() || isResultShown || fullExpression == "0") return
        val cleanExpr = if (fullExpression.endsWith(" ")) fullExpression.dropLast(3) else fullExpression
        
        try {
            val result = evaluate(cleanExpr)
            tvExpression.text = "$cleanExpr =" 
            fullExpression = formatResult(result)
            renderDisplay(fullExpression.length) 
            isResultShown = true
        } catch (e: Exception) {
            tvDisplay.setText("Fehler")
            fullExpression = "0"
            isResultShown = true
        }
    }

    private fun onClear() {
        fullExpression = "0"
        tvExpression.text = ""
        renderDisplay(1)
        isResultShown = false
    }

    private fun evaluate(expr: String): BigDecimal {
        // Парсер математики остался прежним (работает через BigDecimal)
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
