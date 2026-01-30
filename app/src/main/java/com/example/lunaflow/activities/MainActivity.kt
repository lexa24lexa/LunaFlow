package com.example.lunaflow.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.AdviceAdapter
import com.example.lunaflow.adapters.SymptomAdviceAdapter
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.UserLog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.example.lunaflow.utils.NotificationHelper
import android.Manifest

class MainActivity : BaseActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView
    private lateinit var adviceRecyclerView: RecyclerView
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var calendarMonthYear: TextView
    private lateinit var frequentSymptomRecyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()

    private var cachedCycleRecords: List<CycleRecord> = emptyList()

    private var displayedCalendar = Calendar.getInstance()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                sendAllNotifications()
            } else {
                // Permission denied
            }
        }

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

        // Bind views
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

        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            setupCalendar()
        }

        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            setupCalendar()
        }

        // Fetch cycle records and populate UI
        fetchCycleRecordsAndSetupCalendar()

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    sendAllNotifications()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            sendAllNotifications()
        }
    }

    private fun sendAllNotifications() {
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.sendNotification(this, "Your period is late")
        NotificationHelper.sendNotification(this, "Period in 3 days")
        NotificationHelper.sendNotification(this, "Cycle is 3-8 days, looks normal")
    }

    // ---------------- FETCH CYCLE RECORDS ----------------
    private fun fetchCycleRecordsAndSetupCalendar() {
        val currentUser = auth.currentUser ?: return
        Log.d("DEBUG", "Current UID: ${currentUser.uid}")

        db.collection("users")
            .document(currentUser.uid)
            .collection("cycle_records")
            .get()
            .addOnSuccessListener { snapshot ->
                cachedCycleRecords = snapshot.documents.mapNotNull { doc ->
                    try { doc.toObject(CycleRecord::class.java) } catch (e: Exception) { null }
                }.sortedBy { it.date }

                cachedCycleRecords.forEach { Log.d("DEBUG", "CycleRecord: date=${it.date}, phase=${it.phase}, flow=${it.flow}, manual=${it.isManual}") }

                handleCurrentPhaseAndNextCycle()
                setupCalendar()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cycle records.", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- CURRENT PHASE + NEXT CYCLE ----------------
    private fun handleCurrentPhaseAndNextCycle() {
        if (cachedCycleRecords.isEmpty()) {
            currentPhaseText.text = "No cycle data yet"
            nextCycleText.text = "Log your flow to start tracking your cycle"
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val today = sdf.parse(sdf.format(Date())) ?: return

        val menstruationRecords = cachedCycleRecords.filter { it.phase.lowercase() == "menstruation" && it.isManual }
        var start: Date? = null
        var end: Date? = null
        var duration = 0

        if (menstruationRecords.isNotEmpty()) {
            end = sdf.parse(menstruationRecords.last().date)
            var current = end
            var index = menstruationRecords.size - 1
            while (index >= 0 && current != null) {
                val recordDate = sdf.parse(menstruationRecords[index].date)
                val diff = (current.time - recordDate.time) / (1000 * 60 * 60 * 24)
                if (diff <= 1) {
                    start = recordDate
                    duration++
                    current = recordDate
                    index--
                } else break
            }
        }

        val manualMenstruation = cachedCycleRecords.lastOrNull { it.phase.lowercase() == "menstruation" && it.isManual }
// ---------------- CALCULO DE MENSTRUACAO ----------------
        val isMenstruating = menstruationRecords.any { record ->
            val recordDate = sdf.parse(record.date) ?: return@any false
            val flowDuration = when (record.flow?.lowercase()) {
                "light" -> 3
                "medium" -> 5
                "heavy" -> 7
                else -> 5
            }
            val endDate = Calendar.getInstance().apply { time = recordDate; add(Calendar.DAY_OF_MONTH, flowDuration - 1) }.time
            !today.before(recordDate) && !today.after(endDate)
        }

        val currentPhase = if (isMenstruating) "Menstruation" else resolvePhaseForDate(sdf.format(today))
        Log.d("DEBUG", "Current phase calculated: $currentPhase")

        currentPhaseText.text = "You are in $currentPhase Phase"

        fetchAdviceForPhase(currentPhase)
        fetchFrequentSymptoms(currentPhase)

        if (start != null) {
            val cal = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_MONTH, 28) }
            val daysLeft = ((cal.timeInMillis - Calendar.getInstance().timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            nextCycleText.text =
                "Next cycle in $daysLeft days, scheduled for ${SimpleDateFormat("MMMM dd", Locale.ENGLISH).format(cal.time)}"
        } else {
            nextCycleText.text = "Next cycle date unavailable"
        }
    }

    // ---------------- CALENDAR ----------------
    private fun setupCalendar() {
        val calendar = displayedCalendar.clone() as Calendar
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendarMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(calendar.time)

        calendarDaysGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Espaços em branco antes do primeiro dia
        for (i in 1 until firstDayOfWeek) {
            val blankView = TextView(this)
            blankView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            calendarDaysGrid.addView(blankView)
        }

        val recordsMap = cachedCycleRecords.associateBy { it.date }

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val record = recordsMap[dateStr]

            val phase = record?.phase ?: resolvePhaseForDate(dateStr)

            val dayPhaseColor = when (phase.lowercase()) {
                "menstruation" -> Color.parseColor("#FF6B6B")
                "follicular" -> Color.parseColor("#4ECDC4")
                "ovulation" -> Color.parseColor("#FFE66D")
                "luteal" -> Color.parseColor("#C1492E")
                else -> Color.LTGRAY
            }

            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)

                // Fundo arredondado independente para cada dia
                val bg = ContextCompat.getDrawable(context, R.drawable.rounded_background)?.mutate()
                background = bg
                bg?.setTint(dayPhaseColor)

                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))

                // Destaca o dia de hoje
                val today = Calendar.getInstance()
                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day == today.get(Calendar.DAY_OF_MONTH)
                ) {
                    setBackgroundColor(Color.parseColor("#8E44AD"))
                    setTextColor(Color.WHITE)
                }

                setOnClickListener { showDayBottomSheet(dateStr, record) }

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

    // ---------------- PHASE RESOLUTION ----------------
    // ---------------- PHASE RESOLUTION ----------------
    private fun resolvePhaseForDate(dateStr: String): String {
        cachedCycleRecords.find { it.date == dateStr && it.isManual }?.let { return it.phase }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = sdf.parse(dateStr) ?: return "Unknown"

        // Pega todos os registros manuais de menstruação anteriores à data
        val menstruationRecords = cachedCycleRecords.filter {
            it.phase.lowercase() == "menstruation" && it.isManual && it.date <= dateStr
        }.sortedByDescending { it.date }

        if (menstruationRecords.isEmpty()) return "Unknown"

        // Procura o ciclo mais próximo anterior
        val lastMenstruation = menstruationRecords.first()
        val start = sdf.parse(lastMenstruation.date) ?: return "Unknown"
        val diffDays = ((date.time - start.time) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays in 0..4 -> "Menstruation"
            diffDays in 5..13 -> "Follicular"
            diffDays in 14..15 -> "Ovulation"
            diffDays in 16..27 -> "Luteal"
            else -> "Unknown"
        }
    }

    // ---------------- FETCH ADVICE ----------------
    private fun fetchAdviceForPhase(phase: String) {
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)
        Log.d("DEBUG", "Fetching advices for phase: $phase")

        db.collection("advices")
            .whereEqualTo("phase", phase.replaceFirstChar { it.uppercase() })
            .get()
            .addOnSuccessListener { result ->
                val advices = result.documents.mapNotNull { it.toObject(com.example.lunaflow.models.Advice::class.java) }

                val adviceToShow = if (advices.isNotEmpty()) {
                    val lastIndex = prefs.getInt("last_advice_index_$phase", 0)
                    prefs.edit { putInt("last_advice_index_$phase", lastIndex + 1) }
                    advices[lastIndex % advices.size]
                } else com.example.lunaflow.models.Advice(phase, "No advices available")

                adviceRecyclerView.adapter = AdviceAdapter(listOf(adviceToShow))
            }
            .addOnFailureListener {
                adviceRecyclerView.adapter = AdviceAdapter(listOf(com.example.lunaflow.models.Advice(phase, "Failed to load advice.")))
            }
    }

    // ---------------- FETCH FREQUENT SYMPTOMS ----------------
    private fun fetchFrequentSymptoms(phase: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid).collection("logs")
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

                if (logs.isEmpty() || symptomFrequency.isEmpty()) {
                    frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(listOf("No symptoms registered"))
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

    // ---------------- GET SYMPTOM ADVICE ----------------
    private fun getSymptomAdvice(phase: String, symptom: String, callback: (String) -> Unit) {
        db.collection("symptom_advices")
            .whereEqualTo("phase", phase.lowercase())
            .whereEqualTo("symptom", symptom.lowercase())
            .get()
            .addOnSuccessListener { result ->
                val advice = if (result.documents.isNotEmpty()) result.documents[0].getString("advice") ?: "No advice available." else "No advice available."
                callback(advice)
            }
            .addOnFailureListener { callback("No advice available.") }
    }

    // ---------------- BOTTOM SHEET ----------------
    private fun showDayBottomSheet(dateStr: String, record: CycleRecord?) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_day_details, null)

        val phaseText = view.findViewById<TextView>(R.id.bottomSheetPhase)
        val logsText = view.findViewById<TextView>(R.id.bottomSheetLogs)
        val symptomsText = view.findViewById<TextView>(R.id.bottomSheetSymptoms)

        if (record != null) {
            // Phase
            phaseText.text = "Phase: ${record.phase}"

            // Activity Logs
            val activityLogs = record.logs.filter { it.value == true && it.key != "flow" && it.key != "otherSymptoms" }
                .keys.map { it.replaceFirstChar { c -> c.uppercase() } }
            logsText.text = if (activityLogs.isNotEmpty()) activityLogs.joinToString("\n") else "No activity logs"

            // Symptoms + Flow
            val activeSymptoms = record.symptoms.filter { it.value }.keys.map { it.replaceFirstChar { c -> c.uppercase() } }
            val symptomOutput = mutableListOf<String>()

            // FLOW: only display if Light, Medium, or Heavy
            val flowFromRecord = record.flow
            val flowFromLogs = record.logs["flow"] as? String
            val validFlows = listOf("Light", "Medium", "Heavy")
            val flowToShow = flowFromRecord.takeIf { it in validFlows }
                ?: flowFromLogs.takeIf { it in validFlows }

            flowToShow?.let {
                symptomOutput.add("Flow: $it")
            }

            if (activeSymptoms.isNotEmpty()) {
                symptomOutput.add("Symptoms: ${activeSymptoms.joinToString(", ")}")
            }

            symptomsText.text = if (symptomOutput.isNotEmpty()) symptomOutput.joinToString("\n") else "No symptoms"

        } else {
            phaseText.text = "No record for this day"
            logsText.text = ""
            symptomsText.text = ""
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
}
