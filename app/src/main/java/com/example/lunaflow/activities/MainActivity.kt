package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.example.lunaflow.R
import com.example.lunaflow.models.Advice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView
    private lateinit var adviceTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav()

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentPhaseText = findViewById(R.id.currentPhase)
        nextCycleText = findViewById(R.id.nextCycle)
        adviceTextView = findViewById(R.id.adviceText)

        val currentPhase = "Luteal"

        val nextCycleDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 14) }.time
        val formatter = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        val nextCycleStr = formatter.format(nextCycleDate)

        currentPhaseText.text = "You are in $currentPhase Phase"
        nextCycleText.text = "Next cycle in 14 days, scheduled for $nextCycleStr"

        fetchAdviceForPhase(currentPhase)
    }

    private fun fetchAdviceForPhase(phase: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("advices")
            .whereEqualTo("phase", phase)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val advice = result.documents[0].toObject(Advice::class.java)
                    advice?.let {
                        adviceTextView.text = it.adviceText
                    }
                } else {
                    adviceTextView.text = "No advice available."
                }
            }
            .addOnFailureListener {
                adviceTextView.text = "Failed to load advice."
            }
    }
}
