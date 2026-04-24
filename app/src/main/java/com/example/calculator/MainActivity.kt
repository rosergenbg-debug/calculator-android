package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    
    private var fullExpression = ""
    private var isResultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        
        tvExpression.text = ""
        tvDisplay.text = "0"
        
        setupButtons()
    }

    private fun setupButtons() {
        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, 
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        ids.forEachIndexed { i, id -> findViewById<View>(id).setOnClickListener { appendSymbol(i.toString()) } }
        
        findViewById<View>(R.id.btnDecimal).setOnClickListener { appendSymbol(".") }
        findViewById<View>(R.id.btnPlus).setOnClickListener { appendSymbol(" + ") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { appendSymbol(" − ") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { appendSymbol(" × ") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { appendSymbol(" ÷ ") }
        
        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        
        // Оживляем кнопки смены знака и процента
        findViewById<View>(R.id.btnSign).setOnClickListener { onSign() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { appendSymbol("%") }
    }

    private fun appendSymbol(symbol: String) {
        if (isResultShown) {
            if (symbol.contains(Regex("[0-9]"))) {
                fullExpression = symbol
            } else {
                fullExpression = tvDisplay.text.toString().replace(',', '.') + symbol
            }
            isResultShown = false
        } else {
            if (fullExpression == "0" && !symbol.contains(" ")) {
                fullExpression = symbol
            } else {
                fullExpression += symbol
            }
        }
        tvExpression.text = "" 
        renderDisplay()
    }

    private fun renderDisplay() {
        val display = fullExpression.replace('.', ',')
        tvDisplay.text = if (display.isEmpty()) "0" else display
    }

    private fun onEquals() {
        if (fullExpression.isEmpty() || isResultShown) return
        
        try {
            val result = evaluate(fullExpression)
            tvExpression.text = "$fullExpression =" 
            fullExpression = formatResult(result)
            renderDisplay() 
            isResultShown = true
        } catch (e: Exception) {
            tvDisplay.text = "Ошибка"
            fullExpression = ""
        }
    }

    private fun onClear() {
        fullExpression = ""
        tvExpression.text = ""
        tvDisplay.text = "0"
        isResultShown = false
    }

    private fun onSign() {
        if (isResultShown) {
            val current = tvDisplay.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
            fullExpression = formatResult(current * -1)
            isResultShown = false
            renderDisplay()
            return
        }
        
        if (fullExpression.isEmpty()) {
            fullExpression = "-"
            renderDisplay()
            return
        }
        
        val tokens = fullExpression.split(" ").toMutableList()
        val lastToken = tokens.last()
        
        if (lastToken.isNotEmpty() && !lastToken.matches(Regex("[+−×÷]"))) {
            if (lastToken.startsWith("-")) {
                tokens[tokens.lastIndex] = lastToken.substring(1)
            } else {
                tokens[tokens.lastIndex] = "-$lastToken"
            }
            fullExpression = tokens.joinToString(" ")
            renderDisplay()
        } else if (lastToken.isEmpty() || lastToken.matches(Regex("[+−×÷]"))) {
            fullExpression += "-"
            renderDisplay()
        }
    }

    // Умный парсер, который понимает логику карманных калькуляторов
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
                    // Прибавляем/вычитаем процент от ТЕКУЩЕГО результата
                    nextVal = result * (nextVal / 100.0)
                } else {
                    // Умножаем/делим просто на процентную долю
                    nextVal = nextVal / 100.0
                }
            }
            
            result = when (op) {
                "+" -> result + nextVal
                "−", "-" -> result - nextVal
                "×", "*" -> result * nextVal
                "÷", "/" -> if (nextVal != 0.0) result / nextVal else Double.NaN
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
        if (v.isNaN()) return "Ошибка"
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
