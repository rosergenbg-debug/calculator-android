package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    
    private var fullExpression = "0"
    private var isResultShown = false
    private val MAX_DIGITS = 50 // Новый лимит: 50 цифр на одно число
    
    // Внутренняя точность вычислений (до 30 знаков, чтобы не зависнуть на дробях вроде 1/3)
    private val mathContext = MathContext(30, RoundingMode.HALF_UP)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        
        tvExpression.text = ""
        renderDisplay()
        
        setupButtons()
    }

    private fun setupButtons() {
        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, 
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        ids.forEachIndexed { i, id -> findViewById<View>(id).setOnClickListener { appendDigit(i.toString()) } }
        
        findViewById<View>(R.id.btnDecimal).setOnClickListener { appendDecimal() }
        findViewById<View>(R.id.btnPlus).setOnClickListener { appendOperator(" + ") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { appendOperator(" − ") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { appendOperator(" × ") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { appendOperator(" ÷ ") }
        
        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.btnBackspace).setOnClickListener { onBackspace() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { appendPercent() }
    }

    private fun checkErrorState() {
        if (tvDisplay.text == "Fehler") {
            onClear()
        }
    }

    private fun appendDigit(digit: String) {
        checkErrorState()
        if (isResultShown) {
            fullExpression = digit
            isResultShown = false
        } else {
            val tokens = fullExpression.split(" ")
            val lastToken = tokens.last().replace(",", "").replace(".", "")
            if (lastToken.length >= MAX_DIGITS) return // Блокировка после 50 цифр

            if (fullExpression == "0") {
                fullExpression = digit
            } else {
                fullExpression += digit
            }
        }
        tvExpression.text = ""
        renderDisplay()
    }

    private fun appendDecimal() {
        checkErrorState()
        if (isResultShown) {
            fullExpression = "0."
            isResultShown = false
        } else {
            val tokens = fullExpression.split(" ")
            val lastToken = tokens.last()
            
            if (lastToken.contains(".")) return
            
            if (lastToken.isEmpty() || lastToken.matches(Regex(".*[+−×÷].*"))) {
                fullExpression += "0."
            } else {
                fullExpression += "."
            }
        }
        tvExpression.text = ""
        renderDisplay()
    }

    private fun appendOperator(op: String) {
        checkErrorState()
        if (isResultShown) {
            fullExpression = tvDisplay.text.toString().replace(',', '.') + op
            isResultShown = false
        } else {
            if (fullExpression.endsWith(" ")) {
                fullExpression = fullExpression.dropLast(3) + op
            } else {
                fullExpression += op
            }
        }
        tvExpression.text = ""
        renderDisplay()
    }

    private fun appendPercent() {
        checkErrorState()
        val tokens = fullExpression.split(" ")
        if (tokens.last().isNotEmpty() && !tokens.last().endsWith("%") && !fullExpression.endsWith(" ")) {
            fullExpression += "%"
            renderDisplay()
        }
    }

    private fun onBackspace() {
        checkErrorState()
        if (isResultShown) {
            onClear()
            return
        }
        
        if (fullExpression.isNotEmpty() && fullExpression != "0") {
            if (fullExpression.endsWith(" ")) {
                fullExpression = fullExpression.dropLast(3)
            } else {
                fullExpression = fullExpression.dropLast(1)
            }
            
            if (fullExpression.isEmpty()) {
                fullExpression = "0"
            }
            renderDisplay()
        }
    }

    private fun renderDisplay() {
        val display = fullExpression.replace('.', ',')
        tvDisplay.text = display
    }

    private fun onEquals() {
        if (fullExpression.isEmpty() || isResultShown || fullExpression == "0") return
        
        val cleanExpr = if (fullExpression.endsWith(" ")) fullExpression.dropLast(3) else fullExpression
        
        try {
            val result = evaluate(cleanExpr)
            tvExpression.text = "$cleanExpr =" 
            fullExpression = formatResult(result)
            renderDisplay() 
            isResultShown = true
        } catch (e: Exception) {
            tvDisplay.text = "Fehler"
            fullExpression = "0"
            isResultShown = true
        }
    }

    private fun onClear() {
        fullExpression = "0"
        tvExpression.text = ""
        tvDisplay.text = "0"
        isResultShown = false
    }

    // Полностью новое математическое ядро на BigDecimal
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
                if (op == "+" || op == "−" || op == "-") {
                    nextVal = result.multiply(percentDecimal, mathContext)
                } else {
                    nextVal = percentDecimal
                }
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
        // Убираем лишние нули в конце и переводим в строгий текстовый формат (без буквы E)
        var resultStr = v.stripTrailingZeros().toPlainString()
        if (resultStr == "0.0") resultStr = "0"
        return resultStr
    }
}
