package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    
    private var fullExpression = "0"
    private var isResultShown = false

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
        if (tvDisplay.text == "Ошибка") {
            onClear()
        }
    }

    private fun appendDigit(digit: String) {
        checkErrorState()
        if (isResultShown) {
            fullExpression = digit
            isResultShown = false
        } else {
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
            
            // Если в текущем числе уже есть точка, ничего не делаем (защита от 1.2.3)
            if (lastToken.contains(".")) return
            
            // Если перед этим был знак или пустота, добавляем "0."
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
            // Защита от дублирования знаков: если в конце уже знак, меняем его на новый
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
            // Если удаляем знак операции, удаляем сразу 3 символа (" + ")
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
        
        // Если в конце завис знак математики без числа (например "5 + "), игнорируем его
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

    private fun evaluate(expr: String): Double {
        val tokens = expr.trim().split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return 0.0
        
        var result = parseFirstToken(tokens[0])
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextStr = tokens.getOrNull(i + 1) ?: "0"
            
            val isPercent = nextStr.endsWith("%")
            val cleanNextStr = nextStr.replace("%", "").replace(',', '.')
            var nextVal = cleanNextStr.toDoubleOrNull() ?: 0.0
            
            if (isPercent) {
                if (op == "+" || op == "−" || op == "-") {
                    nextVal = result * (nextVal / 100.0)
                } else {
                    nextVal = nextVal / 100.0
                }
            }
            
            result = when (op) {
                "+" -> result + nextVal
                "−", "-" -> result - nextVal
                "×", "*" -> result * nextVal
                "÷", "/" -> if (nextVal != 0.0) result / nextVal else throw ArithmeticException("Div by zero")
                else -> result
            }
            i += 2
        }
        return result
    }

    private fun parseFirstToken(token: String): Double {
        val isPercent = token.endsWith("%")
        val clean = token.replace("%", "").replace(',', '.')
        val v = clean.toDoubleOrNull() ?: 0.0
        return if (isPercent) v / 100.0 else v
    }

    private fun formatResult(v: Double): String {
        if (v.isNaN() || v.isInfinite()) throw ArithmeticException("Invalid math")
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
