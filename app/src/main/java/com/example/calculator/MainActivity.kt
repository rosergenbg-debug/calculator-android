package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var expression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)

        val buttons = listOf(
            "0","1","2","3","4","5","6","7","8","9",
            "+","−","×","÷"
        )

        buttons.forEach { txt ->
            val id = resources.getIdentifier("button$txt", "id", packageName)
            val btn = findViewById<Button?>(id)
            btn?.setOnClickListener { append(txt) }
        }

        findViewById<Button>(resources.getIdentifier("button=", "id", packageName))
            ?.setOnClickListener { calculate() }

        findViewById<Button>(resources.getIdentifier("buttonC", "id", packageName))
            ?.setOnClickListener { clear() }
    }

    private fun append(value: String) {
        expression += value
        display.text = expression
    }

    private fun calculate() {
        try {
            val exp = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")

            val result = eval(exp)
            display.text = result.toString()
            expression = result.toString()
        } catch (e: Exception) {
            display.text = "Error"
            expression = ""
        }
    }

    private fun clear() {
        expression = ""
        display.text = "0"
    }

    private fun eval(str: String): Double {
        return javax.script.ScriptEngineManager()
            .getEngineByName("rhino")
            .eval(str).toString().toDouble()
    }
}
