package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private var first = 0.0
    private var operator = ""
    private var newInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)

        val buttons = listOf(
            R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,
            R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9
        )

        buttons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                pressNumber((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { setOp("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { setOp("-") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { setOp("×") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { setOp("÷") }

        findViewById<Button>(R.id.btnEq).setOnClickListener { calculate() }
        findViewById<Button>(R.id.btnC).setOnClickListener { clear() }
    }

    private fun pressNumber(num: String) {
        if (newInput) {
            display.text = num
            newInput = false
        } else {
            display.append(num)
        }
    }

    private fun setOp(op: String) {
        first = display.text.toString().toDouble()
        operator = op
        newInput = true
    }

    private fun calculate() {
        val second = display.text.toString().toDouble()

        val result = when (operator) {
            "+" -> first + second
            "-" -> first - second
            "×" -> first * second
            "÷" -> if (second != 0.0) first / second else 0.0
            else -> second
        }

        display.text = result.toString()
        newInput = true
    }

    private fun clear() {
        display.text = "0"
        first = 0.0
        operator = ""
        newInput = true
    }
}
