package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    
    private var firstNum = ""
    private var operator = ""
    private var secondNum = ""
    private var isResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        
        // Скрываем верхнюю строку, она нам больше не нужна
        tvExpression.visibility = View.GONE
        updateDisplay()
        
        setupButtons()
    }

    private fun updateDisplay() {
        val opDisplay = if (operator.isNotEmpty()) " $operator " else ""
        var text = firstNum + opDisplay + secondNum
        
        // Меняем системную точку на привычную запятую только для красоты на экране
        text = text.replace('.', ',')
        tvDisplay.text = if (text.isEmpty()) "0" else text
    }

    private fun setupButtons() {
        val nums = listOf(R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9)
        nums.forEachIndexed { i, id -> findViewById<View>(id).setOnClickListener { onDigit(i.toString()) } }
        
        findViewById<View>(R.id.btnDecimal).setOnClickListener { onDecimal() }
        findViewById<View>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { onOperator("−") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { onOperator("×") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { onOperator("÷") }
        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.btnSign).setOnClickListener { onSign() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { onPercent() }
    }

    private fun onDigit(digit: String) {
        if (isResult) {
            firstNum = ""
            isResult = false
        }
        if (operator.isEmpty()) {
            if (firstNum == "0") firstNum = digit else firstNum += digit
        } else {
            if (secondNum == "0") secondNum = digit else secondNum += digit
        }
        updateDisplay()
    }

    private fun onDecimal() {
        if (isResult) {
            firstNum = "0."
            isResult = false
        } else if (operator.isEmpty()) {
            if (firstNum.isEmpty() || firstNum == "-") firstNum += "0."
            else if (!firstNum.contains(".")) firstNum += "."
        } else {
            if (secondNum.isEmpty() || secondNum == "-") secondNum += "0."
            else if (!secondNum.contains(".")) secondNum += "."
        }
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (firstNum.isEmpty() || firstNum == "-") firstNum = "0"
        if (operator.isNotEmpty() && secondNum.isNotEmpty() && secondNum != "-") {
            onEquals() // Если жмем плюс после выражения (1+2+...), считаем первую часть
        }
        operator = op
        isResult = false
        updateDisplay()
    }

    private fun onEquals() {
        if (firstNum.isEmpty() || operator.isEmpty() || secondNum.isEmpty() || secondNum == "-") return
        
        val a = firstNum.toDoubleOrNull() ?: 0.0
        val b = secondNum.toDoubleOrNull() ?: 0.0
        val res = when (operator) {
            "+" -> a + b
            "−", "-" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            else -> 0.0
        }
        
        firstNum = if (res.isNaN()) "Ошибка" else formatDouble(res)
        operator = ""
        secondNum = ""
        isResult = true
        updateDisplay()
    }

    private fun onClear() {
        firstNum = ""
        operator = ""
        secondNum = ""
        isResult = false
        updateDisplay()
    }

    private fun onSign() {
        if (operator.isEmpty()) {
            if (firstNum.isEmpty()) firstNum = "-"
            else if (firstNum.startsWith("-")) firstNum = firstNum.substring(1)
            else firstNum = "-$firstNum"
        } else {
            if (secondNum.isEmpty()) secondNum = "-"
            else if (secondNum.startsWith("-")) secondNum = secondNum.substring(1)
            else secondNum = "-$secondNum"
        }
        updateDisplay()
    }

    private fun onPercent() {
        if (operator.isEmpty()) {
            val v = firstNum.toDoubleOrNull() ?: return
            firstNum = formatDouble(v / 100)
        } else if (secondNum.isNotEmpty() && secondNum != "-") {
            val v = secondNum.toDoubleOrNull() ?: return
            secondNum = formatDouble(v / 100)
        }
        updateDisplay()
    }

    private fun formatDouble(v: Double): String {
        if (v == kotlin.math.floor(v) && !v.isInfinite()) {
            return v.toLong().toString()
        }
        return v.toString()
    }
}
