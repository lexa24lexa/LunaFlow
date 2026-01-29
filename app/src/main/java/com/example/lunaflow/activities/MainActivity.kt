package com.example.lunaflow.activities

import android.annotation.SuppressLint
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
import com.example.lunaflow.adapters.SymptomAdviceAdapter
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.UserLog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView
    private lateinit var adviceRecyclerView: RecyclerView
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var calendarMonthYear: TextView
    private lateinit var frequentSymptomRecyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentLayout(R.layout.activity_main)
        setToolbarTitle("LunaFlow")
        showToolbar(true)
        showBottomNav(true)
        setupBottomNav(R.id.nav_home)

        currentPhaseText = findViewById(R.id.currentPhase)
        nextCycleText = findViewById(R.id.nextCycle)
        adviceRecyclerView = findViewById(R.id.adviceRecyclerView)
        calendarDaysGrid = findViewById(R.id.calendarDaysGrid)
        calendarMonthYear = findViewById(R.id.calendarMonthYear)
        frequentSymptomRecyclerView = findViewById(R.id.frequentSymptomRecyclerView)

        adviceRecyclerView.layoutManager = LinearLayoutManager(this)
        adviceRecyclerView.setHasFixedSize(true)

        frequentSymptomRecyclerView.layoutManager = LinearLayoutManager(this)
        frequentSymptomRecyclerView.setHasFixedSize(true)

        findViewById<FloatingActionButton>(R.id.btnLogChoice).setOnClickListener {
            startActivity(Intent(this, LogChoiceActivity::class.java))
        }

        val currentPhase = "Luteal"
        val nextCycleDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 14) }.time

        currentPhaseText.text = "You are in $currentPhase Phase"
        nextCycleText.text =
            "Next cycle in 14 days, scheduled for ${SimpleDateFormat("MMMM dd", Locale.ENGLISH).format(nextCycleDate)}"

        fetchAdviceForPhase(currentPhase)
        fetchFrequentSymptoms(currentPhase)
        fetchCycleRecordsAndSetupCalendar()
    }

    private fun fetchAdviceForPhase(phase: String) {
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)

        db.collection("advices")
            .whereEqualTo("phase", phase)
            .get()
            .addOnSuccessListener { result ->
                val advices = result.documents.mapNotNull {
                    it.toObject(com.example.lunaflow.models.Advice::class.java)
                }

                val adviceToShow = if (advices.isNotEmpty()) {
                    val lastIndex = prefs.getInt("last_advice_index_$phase", 0)
                    prefs.edit().putInt("last_advice_index_$phase", lastIndex + 1).apply()
                    advices[lastIndex % advices.size]
                } else {
                    com.example.lunaflow.models.Advice(phase, "No advice available.")
                }

                adviceRecyclerView.adapter = AdviceAdapter(listOf(adviceToShow))
            }
            .addOnFailureListener {
                adviceRecyclerView.adapter =
                    AdviceAdapter(listOf(com.example.lunaflow.models.Advice(phase, "Failed to load advice.")))
            }
    }

    private fun fetchFrequentSymptoms(phase: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("logs")
            .whereEqualTo("type", "symptoms")
            .get()
            .addOnSuccessListener { result ->
                val logs = result.documents.mapNotNull { it.toObject(UserLog::class.java) }
                val symptomFrequency = mutableMapOf<String, Int>()

                logs.forEach { log ->
                    log.symptoms.forEach { (symptom, hasSymptom) ->
                        if (hasSymptom) {
                            val key = symptom.lowercase()
                            symptomFrequency[key] = symptomFrequency.getOrDefault(key, 0) + 1
                        }
                    }
                }

                if (symptomFrequency.isEmpty()) {
                    frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(listOf("No symptoms logged yet."))
                    return@addOnSuccessListener
                }

                val symptomAdvices = mutableListOf<String>()
                symptomFrequency.keys.forEach { symptom ->
                    getSymptomAdvice(phase, symptom) { advice ->
                        symptomAdvices.add("Symptom: ${symptom.replaceFirstChar { it.uppercase() }}\nAdvice: $advice")
                        frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(symptomAdvices)
                    }
                }
            }
            .addOnFailureListener {
                frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(listOf("Failed to load symptoms."))
            }
    }

    private fun getSymptomAdvice(phase: String, symptom: String, callback: (String) -> Unit) {
        db.collection("symptom_advices")
            .whereEqualTo("phase", phase.lowercase())
            .whereEqualTo("symptom", symptom.lowercase())
            .get()
            .addOnSuccessListener { result ->
                val advice = if (result.documents.isNotEmpty()) {
                    result.documents[0].getString("advice") ?: "No advice available."
                } else {
                    "No advice available."
                }
                callback(advice)
            }
            .addOnFailureListener {
                callback("No advice available.")
            }
    }

    private fun fetchCycleRecordsAndSetupCalendar() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("cycle_records")
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CycleRecord::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                setupCalendar(records)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cycle records.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCalendar(cycleRecords: List<CycleRecord>) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendarMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(calendar.time)

        calendarDaysGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            val blankView = TextView(this)
            blankView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            calendarDaysGrid.addView(blankView)
        }

        val recordsMap = cycleRecords.associateBy { it.date }

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val record = recordsMap[dateStr]

            val dayPhaseColor = when (record?.phase?.lowercase()) {
                "menstruation" -> Color.parseColor("#FF6B6B")
                "follicular" -> Color.parseColor("#4ECDC4")
                "ovulation" -> Color.parseColor("#FFE66D")
                "luteal" -> Color.parseColor("#C1492E")
                else -> Color.LTGRAY
            }

            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                setBackgroundColor(dayPhaseColor)
                setTextColor(Color.WHITE)

                record?.let {
                    val hasActivity = it.logs?.containsKey("activity") == true
                    val hasSymptoms = !it.symptoms.isNullOrEmpty() && it.symptoms.values.any { value -> value }

                    when {
                        hasActivity -> setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_heart)
                        hasSymptoms -> setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_circle)
                    }
                    compoundDrawablePadding = 8
                }

                val today = Calendar.getInstance()
                if (calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day == today.get(Calendar.DAY_OF_MONTH)
                ) {
                    setBackgroundColor(Color.parseColor("#8E44AD"))
                }

                setOnClickListener {
                    val message = if (record != null) {
                        val logsMsg = if (!record.logs.isNullOrEmpty() && record.logs.containsKey("activity")) "Activities logged" else ""
                        val symptomsMsg = if (!record.symptoms.isNullOrEmpty() && record.symptoms.values.any { it }) "Symptoms logged" else ""
                        "Phase: ${record.phase}\n$logsMsg\n$symptomsMsg"
                    } else {
                        "No record for this day"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
            }

            calendarDaysGrid.addView(dayView)
        }
    }
}
