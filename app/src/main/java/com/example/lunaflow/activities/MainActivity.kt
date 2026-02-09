package com.example.lunaflow.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lunaflow.R
import com.example.lunaflow.adapters.AdviceAdapter
import com.example.lunaflow.adapters.SymptomAdviceAdapter
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.UserCycleProfile
import com.example.lunaflow.utils.NotificationHelper
import com.example.lunaflow.utils.PredictionEngine
import com.example.lunaflow.workers.DailySymptomResetWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private lateinit var currentPhaseText: TextView
    private lateinit var nextCycleText: TextView
    private lateinit var adviceRecyclerView: RecyclerView
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var calendarMonthYear: TextView
    private lateinit var frequentSymptomRecyclerView: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private var displayedCalendar = Calendar.getInstance()
    private lateinit var userProfile: UserCycleProfile
    private var cachedLogs: List<LogEntry> = emptyList()
    private var logsByDate: Map<String, List<LogEntry>> = emptyMap()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) sendAllNotifications()
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        userProfile = loadUserProfile()

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

        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            setupCalendar()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            setupCalendar()
        }

        fetchLogsAndSetupCalendar()

        NotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) sendAllNotifications()
            else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else sendAllNotifications()

        scheduleDailyReset()
    }

    private fun sendAllNotifications() {
        NotificationHelper.sendNotification(this, "Your period is late")
        NotificationHelper.sendNotification(this, "Period in 3 days")
        NotificationHelper.sendNotification(this, "Cycle is 3-8 days, looks normal")
    }

    private fun fetchLogsAndSetupCalendar() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("logs")
            .get()
            .addOnSuccessListener { snapshot ->
                cachedLogs = snapshot.documents.mapNotNull {
                    try { it.toObject(LogEntry::class.java) } catch (e: Exception) { null }
                }
                logsByDate = cachedLogs.groupBy { it.date }

                handleCurrentPhaseAndNextCycle()
                setupCalendar()
                fetchFrequentSymptoms()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load logs.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshLogsAndCalendar() {
        fetchLogsAndSetupCalendar()
    }

    private fun handleCurrentPhaseAndNextCycle() {
        val todayStr = getTodayStr()
        val todayLogs = logsByDate[todayStr] ?: emptyList()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(todayStr)!!

        val currentPhase = PredictionEngine.calculatePhase(today, userProfile, todayLogs)
        currentPhaseText.text = "You are in $currentPhase Phase"
        fetchAdviceForPhase(currentPhase)

        val flowLogs = cachedLogs.filter { it.type.lowercase() == "flow" && it.data["flow"] is String }
        val lastPeriodStartStr = flowLogs.maxByOrNull { it.timestamp }?.date ?: userProfile.lastPeriodStart
        val lastPeriodStart = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(lastPeriodStartStr)!!

        val nextCycleDate = Calendar.getInstance().apply {
            time = lastPeriodStart
            add(Calendar.DAY_OF_MONTH, userProfile.cycleLength)
        }
        val daysLeft = ((nextCycleDate.timeInMillis - Date().time) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

        nextCycleText.text = "Next cycle in $daysLeft days, scheduled for ${
            SimpleDateFormat("MMMM dd", Locale.ENGLISH).format(nextCycleDate.time)
        }"
    }

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

        for (i in 1 until firstDayOfWeek) calendarDaysGrid.addView(TextView(this))

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val dayLogs = logsByDate[dateStr] ?: emptyList()
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateStr)!!
            val phase = PredictionEngine.calculatePhase(dateObj, userProfile, dayLogs)

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
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setBackgroundColor(dayPhaseColor)

                val today = Calendar.getInstance()
                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day == today.get(Calendar.DAY_OF_MONTH)
                ) {
                    setBackgroundColor(Color.parseColor("#8E44AD"))
                    setTextColor(Color.WHITE)
                }

                setOnClickListener { showDayBottomSheet(dateStr, dayLogs) }

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

    private fun fetchFrequentSymptoms() {
        val todayStr = getTodayStr()
        val todayLogs = logsByDate[todayStr] ?: emptyList()

        val prefs = getSharedPreferences("daily_symptoms", MODE_PRIVATE)
        val lastDate = prefs.getString("last_date", null)
        if (lastDate != todayStr) {
            frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(listOf("Please register symptoms today"))
            prefs.edit { putString("last_date", todayStr) }
            return
        }

        val symptomLogs = todayLogs.filter { it.type.lowercase() == "symptoms" }
        val activeSymptoms = mutableListOf<String>()
        symptomLogs.forEach { log ->
            log.data.forEach { (key, value) ->
                if (value is Boolean && value) activeSymptoms.add(key.lowercase())
            }
        }

        if (activeSymptoms.isEmpty()) {
            frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(listOf("Please register symptoms today"))
            return
        }

        val adviceStrings = mutableListOf<String>()
        activeSymptoms.forEach { symptom ->
            db.collection("symptom_advices")
                .whereEqualTo("symptom", symptom)
                .get()
                .addOnSuccessListener { result ->
                    val advice = result.documents.firstOrNull()?.getString("advice") ?: "No advice available"
                    adviceStrings.add("Symptom: ${symptom.replaceFirstChar { it.uppercase() }}\nAdvice: $advice")
                    frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(adviceStrings)
                }
                .addOnFailureListener {
                    adviceStrings.add("Symptom: ${symptom.replaceFirstChar { it.uppercase() }}\nAdvice: Failed to load")
                    frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(adviceStrings)
                }
        }
    }

    private fun fetchAdviceForPhase(phase: String) {
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)

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

    private fun scheduleDailyReset() {
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delay = nextMidnight.timeInMillis - Calendar.getInstance().timeInMillis

        val resetRequest = OneTimeWorkRequestBuilder<DailySymptomResetWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(resetRequest)
    }

    private fun showDayBottomSheet(dateStr: String, logs: List<LogEntry>) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_day_details, null)

        val phaseText = view.findViewById<TextView>(R.id.bottomSheetPhase)
        val logsText = view.findViewById<TextView>(R.id.bottomSheetLogs)
        val symptomsText = view.findViewById<TextView>(R.id.bottomSheetSymptoms)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateObj = sdf.parse(dateStr)!!
        val phase = PredictionEngine.calculatePhase(dateObj, userProfile, logs)
        phaseText.text = "Phase: $phase"

        val activityLogs = logs.filter { it.type.equals("activity", true) }.mapNotNull { logEntry ->
            val activities = logEntry.data.mapNotNull { (key, value) ->
                when (value) {
                    is Boolean -> if (value) key.replaceFirstChar { it.uppercase() } else null
                    is String -> if (value.isNotBlank()) "${key.replaceFirstChar { it.uppercase() }}: $value" else null
                    else -> null
                }
            }
            if (activities.isNotEmpty()) "Activity: ${activities.joinToString(", ")}" else null
        }
        logsText.text = if (activityLogs.isNotEmpty()) activityLogs.joinToString("\n") else "No activity logs"

        val symptomLogs = logs.filter { it.type.lowercase() == "symptoms" }
        val activeSymptoms = mutableListOf<String>()
        var flowToShow: String? = null
        symptomLogs.forEach { logEntry ->
            logEntry.data.forEach { (key, value) ->
                when {
                    key.equals("flow", true) && value is String -> flowToShow = value
                    value is Boolean && value -> activeSymptoms.add(key.replaceFirstChar { it.uppercase() })
                    else -> {}
                }
            }
        }
        val symptomOutput = mutableListOf<String>()
        flowToShow?.let { symptomOutput.add("Flow: $it") }
        if (activeSymptoms.isNotEmpty()) symptomOutput.add("Symptoms: ${activeSymptoms.joinToString(", ")}")
        symptomsText.text = if (symptomOutput.isNotEmpty()) symptomOutput.joinToString("\n") else "No symptoms"

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun getTodayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

    private fun loadUserProfile(): UserCycleProfile {
        val prefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        return UserCycleProfile(
            lastPeriodStart = prefs.getString("lastPeriodStart", "2026-02-01") ?: "2026-02-01",
            cycleLength = prefs.getInt("cycleLength", 28),
            periodLength = prefs.getInt("periodLength", 5)
        )
    }
}
