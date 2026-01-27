package com.example.lunaflow.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R

class LogSymptomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_symptom)

        val saveButton: Button = findViewById(R.id.saveButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)

        saveButton.setOnClickListener {
            Toast.makeText(this, "Symptom saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        cancelButton.setOnClickListener { finish() }
    }
}
