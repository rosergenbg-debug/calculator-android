package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var current = ""
    private var operator = ""
    private var first = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)

        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        setListeners(root)
    }

    private fun setListeners(view: android.view.View) {
        if (view is Button) {
            view.setOnClickListener { handle(view.text.toString()) }
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                setListeners(view.getChildAt(i))
            }
        }
    }

    private fun handle(value: String) {
        when (value) {
            "C" -> {
                current = ""
                first = ""
                operator = ""
                display.text = "0"
            }
            "+", "-", "×", "÷" -> {
                first = current
                operator = value
                current = ""
            }
            "=" -> {
                val result = calculate()
                display.text = result
                current = result
            }
            else -> {
                current += value
                display.text = current
            }
        }
    }

    private fun calculate(): String {
        val a = first.toDoubleOrNull() ?: return "0"
        val b = current.toDoubleOrNull() ?: return "0"

        val res = when (operator) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else 0.0
            else -> 0.0
        }

        return res.toString()
    }
}
