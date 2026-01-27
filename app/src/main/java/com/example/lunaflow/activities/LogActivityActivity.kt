package com.example.lunaflow.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R

class LogActivityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_activity)

        val sexLayout: LinearLayout = findViewById(R.id.sexOptionsLayout)
        val planBLayout: LinearLayout = findViewById(R.id.planBOptionsLayout)

        val sexCheckBox: CheckBox = findViewById(R.id.sex)
        val planBCheckBox: CheckBox = findViewById(R.id.planBPill)

        sexCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sexLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        planBCheckBox.setOnCheckedChangeListener { _, isChecked ->
            planBLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            Toast.makeText(this, "Activity saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }
}
