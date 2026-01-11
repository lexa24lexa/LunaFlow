package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.AdviceAdapter
import com.example.lunaflow.models.Advice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView
    private lateinit var adviceRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentPhaseText = findViewById(R.id.currentPhase)
        nextCycleText = findViewById(R.id.nextCycle)

        adviceRecyclerView = findViewById(R.id.adviceRecyclerView)
        adviceRecyclerView.layoutManager = LinearLayoutManager(this)

        val currentPhase = "Luteal"

        val nextCycleDate = Calendar.getInstance()
            .apply { add(Calendar.DAY_OF_MONTH, 14) }
            .time

        val formatter = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        nextCycleText.text =
            "Next cycle in 14 days, scheduled for ${formatter.format(nextCycleDate)}"

        currentPhaseText.text = "You are in $currentPhase Phase"

        fetchAdviceForPhase(currentPhase)
    }

    private fun fetchAdviceForPhase(phase: String) {
        val db = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)

        db.collection("advices")
            .whereEqualTo("phase", phase)
            .get()
            .addOnSuccessListener { result ->
                val advices = result.documents.mapNotNull {
                    it.toObject(Advice::class.java)
                }

                if (advices.isNotEmpty()) {
                    val lastIndex = prefs.getInt("last_advice_index_$phase", 0)

                    val adviceToShow = advices[lastIndex % advices.size]

                    // mostra só UM advice
                    adviceRecyclerView.adapter =
                        AdviceAdapter(listOf(adviceToShow))

                    // guarda o próximo índice
                    prefs.edit()
                        .putInt("last_advice_index_$phase", lastIndex + 1)
                        .apply()
                } else {
                    adviceRecyclerView.adapter =
                        AdviceAdapter(listOf(Advice(phase, "No advice available.")))
                }
            }
            .addOnFailureListener {
                adviceRecyclerView.adapter =
                    AdviceAdapter(listOf(Advice(phase, "Failed to load advice.")))
            }
    }

}
