package com.example.calculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: EditText
    private lateinit var tvExpression: TextView
    private lateinit var tvPreview: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvAuthorEmail: TextView

    private var fullExpression = "0"
    private var isResultShown = false

    private val maxDigits = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)
        tvExpression = findViewById(R.id.tvExpression)
        tvPreview = findViewById(R.id.tvPreview)
        tvVersion = findViewById(R.id.tvVersion)
        tvAuthorEmail = findViewById(R.id.tvAuthorEmail)

        tvDisplay.showSoftInputOnFocus = false
        setupProjectInfo()

        tvExpression.text = ""
        tvPreview.text = ""
        renderDisplay(fullExpression.length)

        setupButtons()
    }

    private fun setupProjectInfo() {
        val contactEmail = getString(R.string.contact_email)
        tvVersion.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
        tvAuthorEmail.text = getString(R.string.author_signature, contactEmail)
        tvAuthorEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$contactEmail")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            }
            runCatching { startActivity(emailIntent) }
        }
    }

    private fun setupButtons() {
        listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        ).forEachIndexed { digit, id ->
            findViewById<View>(id).setOnClickListener { insertDigit(digit.toString()) }
        }

        findViewById<View>(R.id.btnDecimal).setOnClickListener { insertDecimal() }
        findViewById<View>(R.id.btnPlus).setOnClickListener { insertOperator("+") }
        findViewById<View>(R.id.btnMinus).setOnClickListener { insertOperator("−") }
        findViewById<View>(R.id.btnMultiply).setOnClickListener { insertOperator("×") }
        findViewById<View>(R.id.btnDivide).setOnClickListener { insertOperator("÷") }

        findViewById<View>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<View>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<View>(R.id.btnBackspace).setOnClickListener { onBackspace() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { insertPercent() }
    }

    private fun getCursorPos(): Int {
        val sel = tvDisplay.selectionStart
        return if (sel >= 0) sel.coerceAtMost(fullExpression.length) else fullExpression.length
    }

    private fun insertDigit(digit: String) {
        clearErrorIfNeeded()

        if (isResultShown) {
            fullExpression = digit
            isResultShown = false
            tvExpression.text = ""
            renderDisplay(1)
            updatePreview()
            return
        }

        editAtCursor { pos ->
            val edited = ExpressionEditor.insertDigit(fullExpression, pos, digit.single(), maxDigits)
            fullExpression = edited.expression
            renderDisplay(edited.cursor)
            updatePreview()
        }
    }

    private fun insertDecimal() {
        clearErrorIfNeeded()

        if (isResultShown) {
            fullExpression = "0."
            isResultShown = false
            tvExpression.text = ""
            renderDisplay(2)
            updatePreview()
            return
        }

        editAtCursor { pos ->
            val edited = ExpressionEditor.insertDecimal(fullExpression, pos)
            fullExpression = edited.expression
            renderDisplay(edited.cursor)
            updatePreview()
        }
    }

    private fun insertOperator(operator: String) {
        clearErrorIfNeeded()

        if (isResultShown) {
            isResultShown = false
            tvExpression.text = ""
        }

        editAtCursor { pos ->
            val edited = ExpressionEditor.insertOperator(fullExpression, pos, operator)
            fullExpression = edited.expression
            renderDisplay(edited.cursor)
            updatePreview()
        }
    }

    private fun insertPercent() {
        clearErrorIfNeeded()
        if (isResultShown) {
            isResultShown = false
            tvExpression.text = ""
        }

        editAtCursor { pos ->
            val edited = ExpressionEditor.insertPercent(fullExpression, pos)
            fullExpression = edited.expression
            renderDisplay(edited.cursor)
            updatePreview()
        }
    }

    private fun onBackspace() {
        clearErrorIfNeeded()
        if (isResultShown) {
            onClear()
            return
        }

        val pos = getCursorPos()
        if (pos == 0) return

        val edited = ExpressionEditor.backspace(fullExpression, pos)
        fullExpression = edited.expression
        renderDisplay(edited.cursor)
        updatePreview()
    }

    private fun onEquals() {
        if (fullExpression == "0" || isResultShown) return

        val cleanExpr = CalculatorEngine.normalizeForEvaluation(fullExpression)
        if (cleanExpr.isBlank()) return

        try {
            val result = CalculatorEngine.evaluate(cleanExpr)
            tvExpression.text = "${cleanExpr.replace('.', ',')} ="
            fullExpression = CalculatorEngine.formatResult(result)
            renderDisplay(fullExpression.length)
            tvPreview.text = ""
            isResultShown = true
        } catch (e: Exception) {
            showError()
        }
    }

    private fun onClear() {
        fullExpression = "0"
        tvExpression.text = ""
        tvPreview.text = ""
        renderDisplay(1)
        isResultShown = false
    }

    private fun updatePreview() {
        if (isResultShown || fullExpression == "0" || !CalculatorEngine.hasMathAction(fullExpression)) {
            tvPreview.text = ""
            return
        }

        val cleanExpr = CalculatorEngine.normalizeForEvaluation(fullExpression)
        if (cleanExpr.isBlank() || cleanExpr == "0") {
            tvPreview.text = ""
            return
        }

        try {
            tvPreview.text = "= " + CalculatorEngine.formatResult(CalculatorEngine.evaluate(cleanExpr)).replace('.', ',')
        } catch (e: Exception) {
            tvPreview.text = ""
        }
    }

    private fun renderDisplay(newCursorPos: Int? = null) {
        val currentCursor = getCursorPos()
        val displayStr = fullExpression.replace('.', ',')

        val textSize = when {
            displayStr.length <= 8 -> 64f
            displayStr.length <= 15 -> 48f
            displayStr.length <= 25 -> 36f
            else -> 28f
        }
        tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        tvDisplay.setText(displayStr)

        val finalPos = (newCursorPos ?: currentCursor).coerceIn(0, displayStr.length)
        tvDisplay.setSelection(finalPos)
    }

    private inline fun editAtCursor(block: (pos: Int) -> Unit) {
        block(getCursorPos())
    }

    private fun clearErrorIfNeeded() {
        if (tvDisplay.text.toString() == "Fehler") onClear()
    }

    private fun showError() {
        tvDisplay.setText("Fehler")
        fullExpression = "0"
        tvPreview.text = ""
        isResultShown = true
    }

}
