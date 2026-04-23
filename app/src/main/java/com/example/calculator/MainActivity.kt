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
    private var lastExpression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        
        // Включаем верхнюю строку обратно
        tvExpression.visibility = View.VISIBLE
        updateDisplay()
        
        setupButtons()
    }

    private fun updateDisplay() {
        if (isResult) {
            // Состояние "после нажатия равно": выражение улетает наверх, внизу только ответ
            tvExpression.text = lastExpression.replace('.', ',')
            tvDisplay.text = firstNum.replace('.', ',')
        } else {
            // Состояние "ввода": верхняя строка пустая, всё пишется внизу
            tvExpression.text = ""
            val opDisplay = if (operator.isNotEmpty()) " $operator " else ""
            var text = firstNum + opDisplay + secondNum
            if (text.isEmpty()) text = "0"
            tvDisplay.text = text.replace('.', ',')
        }
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
            // Если сразу после ответа вводим цифру — начинаем всё заново
            firstNum = digit
            operator = ""
            secondNum = ""
            isResult = false
        } else {
            if (operator.isEmpty()) {
                if (firstNum == "0") firstNum = digit else firstNum += digit
            } else {
                if (secondNum == "0") secondNum = digit else secondNum += digit
            }
        }
        updateDisplay()
    }

    private fun onDecimal() {
        if (isResult) {
            firstNum = "0."
            operator = ""
            secondNum = ""
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
            // Если вводим второй оператор (напр. 5 + 3 +), сразу считаем первую часть
            calculate()
        }
        
        operator = op
        isResult = false
        updateDisplay()
    }

    private fun onEquals() {
        if (firstNum.isEmpty() || operator.isEmpty() || secondNum.isEmpty() || secondNum == "-") return
        
        // Запоминаем выражение для верхней строки до того, как посчитаем ответ
        lastExpression = "$firstNum $operator $secondNum ="
        calculate()
        isResult = true
        updateDisplay()
    }

    private fun calculate() {
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
    }

    private fun onClear() {
        firstNum = ""
        operator = ""
        secondNum = ""
        lastExpression = ""
        isResult = false
        updateDisplay()
    }

    private fun onSign() {
        if (isResult) isResult = false 
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
        if (isResult) isResult = false
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
        // Убираем дробную часть, если число целое (5.0 -> 5)
        if (v == kotlin.math.floor(v) && !v.isInfinite()) {
            return v.toLong().toString()
        }
        val str = v.toString()
        return if (str.endsWith(".0")) str.dropLast(2) else str
    }
}
