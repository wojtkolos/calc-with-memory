package com.example.calapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import net.objecthunter.exp4j.ExpressionBuilder
import java.lang.Math.pow
import java.lang.Math.sqrt
import kotlin.math.absoluteValue
import kotlin.math.pow

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    lateinit var txtInput: TextView
    lateinit var textScore: TextView
    var lastNumeric: Boolean = false
    var stateError: Boolean = false
    var lastDot: Boolean = false
    var scored : Boolean = false

    var memoryScore: Double = 0.0

    var result: Double = 0.0
    private var format:String = "Double"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtInput = findViewById(R.id.input_box)
        textScore = findViewById(R.id.result_box)
    }

    private fun clearCalculator() {
        txtInput.text = ""
        textScore.text = ""
        lastNumeric = false
        stateError = false
        lastDot = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settingsitem->startSettingsActivity()
            R.id.shareitem->shareResult()
            else -> return super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private val launchSettingsActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
       if (result.resultCode == RESULT_OK){
           format = result.data?.getStringExtra(getString(R.string.numberFormatKey)).toString()
           showSnackbarBottom(format)
       }
    }

    private fun shareResult() {
        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain").putExtra(Intent.EXTRA_TEXT,getExpressionResult())
        if(shareIntent.resolveActivity(packageManager)!=null){
            startActivity(shareIntent)
        }
    }



    private fun getExpressionResult(): String {
        return txtInput.text.toString() + " = " + if(format === "Integer") textScore.text.toString().toInt().toString() else textScore.text.toString()
    }

    private fun startSettingsActivity() {
        val intent: Intent = Intent(this,SettingsActivity::class.java)
        intent.putExtra(getString(R.string.numberFormatKey),format)
        launchSettingsActivity.launch(intent)
    }


    fun onClickMemory(view: View) {

        when((view as Button).text){
            "MC"->{
                memoryScore = 0.0
                showSnackbarBottom("Memory cleared")
                scored = true
            }
            "MR"->{
                if(lastNumeric && !stateError) {
                    txtInput.text = memoryScore.toString()
                }
                else{
                    txtInput.append(memoryScore.toString())
                }
                scored = true
                showSnackbarBottom("Memory recalled")
            }
            "M+"->{
                memoryScore = (memoryScore + (textScore.text.toString().toDoubleOrNull() ?: 0.0))
                showSnackbarBottom("Added to value in memory")
                scored = true
            }
            "M-"->{
                memoryScore = (memoryScore - (textScore.text.toString().toDoubleOrNull() ?: 0.0))
                showSnackbarBottom("Substracted from value in memory")
                scored = true
            }
            "MS"->{
                memoryScore = textScore.text!!.toString().toDoubleOrNull() ?: 0.0
                showSnackbarBottom("Value stored in memory")
                scored = true
            }

        }
    }
    fun onClickOperator(view: View) {
        if(scored) {
            scored = !scored
            txtInput.text = textScore.text
            textScore.text = ""
        }
        if (lastNumeric && !stateError) {
            val text = txtInput.text.toString()
            val number = calculateResult(text)
            if(number === 0.0) {
                showSnackbarBottom("Operation on zero! Aborting")
                return
            }
            when ((view as Button).text) {
                "1/x" -> {
                    txtInput.text = ("1/(" + text + ")")
                    textScore.text = (1 / number).toString()
                }
                "×^2" -> {
                    txtInput.text = ("(" + text + ")^2")
                    textScore.text = (number.pow(2.0)).toString()
                }
                "√" -> {
                    txtInput.text = ("√(" + text + ")")
                    textScore.text = (kotlin.math.sqrt(number)).toString()
                }
                "|×|" -> {
                    txtInput.text = ("|" + text + "|")
                    textScore.text = (number).absoluteValue.toString()
                }
                "±" -> {
                    txtInput.text = ("-1*(" + text + ")")
                    textScore.text = (number * -1).toString()
                }
                "%" -> {
                    txtInput.text = ("(" + text + ")%")
                    textScore.text = (number * 0.01).toString()
                }
                "π" ->{
                    txtInput.text = ("(" + text + ")π")
                    textScore.text = (number * 3.14).toString()
                }
                else -> {
                    txtInput.append((view as Button).text)
                    lastNumeric = false
                    scored = false
                }
            }

            lastDot = false    // Reset the DOT flag
        }
    }
    fun onClickNumber(view: View) {
        if(scored) {
            scored = !scored
            txtInput.text = ""
            textScore.text = ""
        }
        if (stateError) {
            txtInput.text = (view as Button).text
            stateError = false
        } else {
            txtInput.append((view as Button).text)
        }
        lastNumeric = true
    }
    fun onClearButton(view: View) {
        clearCalculator()
        showSnackbarBottom(getString(R.string.clear_msg))
    }
    fun onClearElementButton(view: View){
        txtInput.text = txtInput!!.text.toString().dropLast(1)
        textScore.text = ""
        lastNumeric = true;
    }
    fun equalresult(view: View) {
        if (lastNumeric && !stateError) {
            if (format === "Integer") {
                textScore.text = calculateResult(txtInput.text.toString()).toInt().toString()
            } else {
                textScore.text = calculateResult(txtInput.text.toString()).toString()

            }
        }
    }

    private fun calculateResult(expression: String) : Double{
        if(expression !== "Infinity") {
            var txt = expression
            txt = txt.replace(
                "×" to "*",
                "\u00F7" to "/",
                "," to ".",
                "π" to "*3.14"
            )

            try {
                val expression = ExpressionBuilder(txt).build()
                val result = expression.evaluate()
                lastDot = true
                scored = true

                return result.toDouble()
            } catch (ex: ArithmeticException) {
                textScore.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
        return 0.0
    }

    private fun String.replace(vararg pairs: Pair<String, String>): String =
        pairs.fold(this) { acc, (old, new) -> acc.replace(old, new) }

    private fun showSnackbarBottom(text: String = "Done"){
        Snackbar.make(
            findViewById(R.id.mainContainer),
            text,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onClick(p0: View?) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(p0: View?): Boolean {
        TODO("Not yet implemented")
    }
}