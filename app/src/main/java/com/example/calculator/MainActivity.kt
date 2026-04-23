package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var current = "0"
    private var storedValue: Double? = null
    private var pendingOp: String? = null
    private var startNewNumber = true
    private val formatter = DecimalFormat("0.########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.tvDisplay)
        display.text = current

        bindDigit(R.id.btn0, "0")
        bindDigit(R.id.btn1, "1")
        bindDigit(R.id.btn2, "2")
        bindDigit(R.id.btn3, "3")
        bindDigit(R.id.btn4, "4")
        bindDigit(R.id.btn5, "5")
        bindDigit(R.id.btn6, "6")
        bindDigit(R.id.btn7, "7")
        bindDigit(R.id.btn8, "8")
        bindDigit(R.id.btn9, "9")

        findViewById<Button>(R.id.btnDot).setOnClickListener { onDot() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { toggleSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { percent() }
        findViewById<Button>(R.id.btnAdd).setOnClickListener { chooseOp("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { chooseOp("-") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { chooseOp("*") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { chooseOp("/") }
        findViewById<Button>(R.id.btnEq).setOnClickListener { calculateResult() }
    }

    private fun bindDigit(id: Int, digit: String) {
        findViewById<Button>(id).setOnClickListener {
            if (startNewNumber) {
                current = digit
                startNewNumber = false
            } else {
                current = if (current == "0") digit else current + digit
            }
            updateDisplay()
        }
    }

    private fun onDot() {
        if (startNewNumber) {
            current = "0."
            startNewNumber = false
        } else if (!current.contains('.')) {
            current += "."
        }
        updateDisplay()
    }

    private fun chooseOp(op: String) {
        val value = current.toDoubleOrNull() ?: 0.0
        if (storedValue == null) {
            storedValue = value
        } else if (!startNewNumber && pendingOp != null) {
            storedValue = applyOp(storedValue!!, value, pendingOp!!)
            current = formatNumber(storedValue!!)
            updateDisplay()
        }
        pendingOp = op
        startNewNumber = true
    }

    private fun calculateResult() {
        val left = storedValue ?: return
        val right = current.toDoubleOrNull() ?: return
        val op = pendingOp ?: return

        val result = applyOp(left, right, op)
        current = if (result.isNaN() || result.isInfinite()) "Error" else formatNumber(result)
        storedValue = null
        pendingOp = null
        startNewNumber = true
        updateDisplay()
    }

    private fun applyOp(a: Double, b: Double, op: String): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) Double.NaN else a / b
            else -> b
        }
    }

    private fun toggleSign() {
        val value = current.toDoubleOrNull() ?: return
        current = formatNumber(-value)
        updateDisplay()
    }

    private fun percent() {
        val value = current.toDoubleOrNull() ?: return
        current = formatNumber(value / 100.0)
        updateDisplay()
    }

    private fun clearAll() {
        current = "0"
        storedValue = null
        pendingOp = null
        startNewNumber = true
        updateDisplay()
    }

    private fun updateDisplay() {
        display.text = current
    }

    private fun formatNumber(value: Double): String {
        return formatter.format(value)
    }
}
