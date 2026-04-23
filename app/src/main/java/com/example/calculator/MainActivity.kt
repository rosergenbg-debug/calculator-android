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
        
        // Пока оставим эти функции простыми для стабильности
        findViewById<View>(R.id.btnSign).setOnClickListener { /* Смена знака в строке — сложная логика, пока пропустим */ }
        findViewById<View>(R.id.btnPercent).setOnClickListener { appendSymbol("%") }
    }

    private fun appendSymbol(symbol: String) {
        if (isResultShown) {
            // Если только что был показан результат и мы нажали цифру — стираем всё
            // Если нажали оператор — продолжаем считать от результата
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
        tvExpression.text = "" // Очищаем верх при новом вводе
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
            tvExpression.text = "$fullExpression =" // Выражение улетает вверх
            fullExpression = formatResult(result)
            renderDisplay() // Результат становится большим внизу
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

    // Простой парсер для вычисления строки
    private fun evaluate(expr: String): Double {
        val tokens = expr.trim().split(" ")
        if (tokens.isEmpty()) return 0.0
        
        var result = tokens[0].replace(',', '.').toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextVal = tokens.getOrNull(i + 1)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            result = when (op) {
                "+" -> result + nextVal
                "−" -> result - nextVal
                "×" -> result * nextVal
                "÷" -> if (nextVal != 0.0) result / nextVal else Double.NaN
                else -> result
            }
            i += 2
        }
        return result
    }

    private fun formatResult(v: Double): String {
        if (v.isNaN()) return "Ошибка"
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
