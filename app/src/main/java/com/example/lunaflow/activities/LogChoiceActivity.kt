package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R

class LogChoiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_choice)

        val symptomButton: Button = findViewById(R.id.symptomButton)
        val activityButton: Button = findViewById(R.id.activityButton)

        symptomButton.setOnClickListener {
            startActivity(Intent(this, LogSymptomActivity::class.java))
        }

        activityButton.setOnClickListener {
            startActivity(Intent(this, LogActivityActivity::class.java))
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
