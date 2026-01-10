package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            // segurança extra: se não houver user, volta ao login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentPhaseText = findViewById(R.id.currentPhase)
        nextCycleText = findViewById(R.id.nextCycle)

        val currentPhase = "Luteal"

        val nextCycleDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 14)
        }.time

        val formatter = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        val nextCycleStr = formatter.format(nextCycleDate)

        currentPhaseText.text = "You are in $currentPhase Phase"
        nextCycleText.text = "Next cycle in 14 days, scheduled for $nextCycleStr"
    }
}
