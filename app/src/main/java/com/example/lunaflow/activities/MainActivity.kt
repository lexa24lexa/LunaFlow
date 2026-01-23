package com.example.lunaflow.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var calendarMonthYear: TextView

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

        calendarDaysGrid = findViewById(R.id.calendarDaysGrid)
        calendarMonthYear = findViewById(R.id.calendarMonthYear)

        val currentPhase = "Luteal"

        val nextCycleDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 14) }.time
        val formatter = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
        nextCycleText.text = "Next cycle in 14 days, scheduled for ${formatter.format(nextCycleDate)}"
        currentPhaseText.text = "You are in $currentPhase Phase"

        fetchAdviceForPhase(currentPhase)
        setupCalendar()
    }

    private fun fetchAdviceForPhase(phase: String) {
        val db = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)

        db.collection("advices")
            .whereEqualTo("phase", phase)
            .get()
            .addOnSuccessListener { result ->
                val advices = result.documents.mapNotNull { it.toObject(Advice::class.java) }
                if (advices.isNotEmpty()) {
                    val lastIndex = prefs.getInt("last_advice_index_$phase", 0)
                    val adviceToShow = advices[lastIndex % advices.size]
                    adviceRecyclerView.adapter = AdviceAdapter(listOf(adviceToShow))
                    prefs.edit().putInt("last_advice_index_$phase", lastIndex + 1).apply()
                } else {
                    adviceRecyclerView.adapter = AdviceAdapter(listOf(Advice(phase, "No advice available.")))
                }
            }
            .addOnFailureListener {
                adviceRecyclerView.adapter = AdviceAdapter(listOf(Advice(phase, "Failed to load advice.")))
            }
    }

    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        calendarMonthYear.text = sdf.format(calendar.time)

        calendarDaysGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // Sunday = 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // blank offsets
        for (i in 1 until firstDayOfWeek) {
            val blankView = TextView(this)
            blankView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 100
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            calendarDaysGrid.addView(blankView)
        }

        // days
        for (day in 1..daysInMonth) {
            val dayView = TextView(this)
            dayView.text = day.toString()
            dayView.gravity = Gravity.CENTER
            dayView.setPadding(8, 8, 8, 8)
            dayView.setBackgroundResource(R.drawable.rounded_background)

            val today = Calendar.getInstance()
            if (calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && day == today.get(Calendar.DAY_OF_MONTH)
            ) {
                dayView.setBackgroundColor(Color.parseColor("#C1492E"))
                dayView.setTextColor(Color.WHITE)
            }

            dayView.setOnClickListener {
                Toast.makeText(this, "Clicked on day $day", Toast.LENGTH_SHORT).show()
            }

            dayView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 100
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }

            calendarDaysGrid.addView(dayView)
        }
    }
}
