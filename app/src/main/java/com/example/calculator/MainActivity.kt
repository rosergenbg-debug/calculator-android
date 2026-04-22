package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    private var currentInput = StringBuilder()
    private var operator = ""
    private var firstOperand = 0.0
    private var isNewInput = false
    private var hasDecimal = false
    private val formatter = DecimalFormat("#,##0.##########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        setupButtons()
    }

    private fun setupButtons() {
        val nums = listOf(R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9)
        nums.forEachIndexed { i, id -> findViewById<Button>(id).setOnClickListener { onDigit(i.toString()) } }
        findViewById<Button>(R.id.btnDecimal).setOnClickListener { onDecimal() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator("−") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperator("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator("÷") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { onToggleSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercent() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { onBackspace() }
    }

    private fun onDigit(digit: String) {
        if (isNewInput) { currentInput.clear(); hasDecimal = false; isNewInput = false }
        if (currentInput.toString() == "0" && digit != ".") currentInput.clear()
        currentInput.append(digit)
        tvDisplay.text = currentInput.toString()
    }

    private fun onDecimal() {
        if (isNewInput) { currentInput.clear(); currentInput.append("0"); isNewInput = false; hasDecimal = false }
        if (!hasDecimal) { if (currentInput.isEmpty()) currentInput.append("0"); currentInput.append("."); hasDecimal = true; tvDisplay.text = currentInput.toString() }
    }

    private fun onOperator(op: String) {
        val current = currentInput.toString().toDoubleOrNull() ?: 0.0
        if (operator.isNotEmpty() && !isNewInput) {
            firstOperand = calculate(firstOperand, current, operator)
            tvExpression.text = "${fmt(firstOperand)} $op"
            tvDisplay.text = fmt(firstOperand)
        } else { firstOperand = current; tvExpression.text = "${fmt(firstOperand)} $op" }
        operator = op; isNewInput = true; hasDecimal = false
    }

    private fun onEquals() {
        if (operator.isEmpty()) return
        val second = currentInput.toString().toDoubleOrNull() ?: return
        val result = calculate(firstOperand, second, operator)
        tvExpression.text = "${tvExpression.text} ${fmt(second)} ="
        tvDisplay.text = fmt(result)
        currentInput.clear(); currentInput.append(fmtRaw(result))
        operator = ""; isNewInput = true; hasDecimal = fmtRaw(result).contains(".")
    }

    private fun calculate(a: Double, b: Double, op: String) = when(op) {
        "+" -> a + b; "−" -> a - b; "×" -> a * b
        "÷" -> if (b != 0.0) a / b else Double.NaN; else -> b
    }

    private fun onClear() { currentInput.clear(); operator = ""; firstOperand = 0.0; isNewInput = false; hasDecimal = false; tvDisplay.text = "0"; tvExpression.text = "" }
    private fun onToggleSign() { val v = currentInput.toString().toDoubleOrNull() ?: return; currentInput.clear(); currentInput.append(fmtRaw(-v)); hasDecimal = currentInput.contains("."); tvDisplay.text = currentInput.toString() }
    private fun onPercent() { val v = currentInput.toString().toDoubleOrNull() ?: return; currentInput.clear(); currentInput.append(fmtRaw(v/100)); hasDecimal = currentInput.contains("."); tvDisplay.text = currentInput.toString() }
    private fun onBackspace() { if (isNewInput) return; if (currentInput.isNotEmpty()) { if (currentInput.last() == '.') hasDecimal = false; currentInput.deleteCharAt(currentInput.length-1) }; tvDisplay.text = if (currentInput.isEmpty()) "0" else currentInput.toString() }
    private fun fmt(v: Double) = if (v.isNaN()) "Ошибка" else formatter.format(v)
    private fun fmtRaw(v: Double) = if (v.isNaN()) "0" else if (v == kotlin.math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
}
