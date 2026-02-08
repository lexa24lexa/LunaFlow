package com.example.lunaflow.activities

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.AdviceAdapter
import com.example.lunaflow.adapters.SymptomAdviceAdapter
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.LogEntry
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) sendAllNotifications()
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // checa login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // layout e toolbar
        setContentLayout(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setToolbarTitle("LunaFlow")

        // bind views
        currentPhaseText = findViewById(R.id.currentPhase)
        nextCycleText = findViewById(R.id.nextCycle)
        adviceRecyclerView = findViewById(R.id.adviceRecyclerView)
        calendarDaysGrid = findViewById(R.id.calendarDaysGrid)
        calendarMonthYear = findViewById(R.id.calendarMonthYear)
        frequentSymptomRecyclerView = findViewById(R.id.frequentSymptomRecyclerView)

        // setup recycler views
        adviceRecyclerView.layoutManager = LinearLayoutManager(this)
        adviceRecyclerView.setHasFixedSize(true)
        frequentSymptomRecyclerView.layoutManager = LinearLayoutManager(this)
        frequentSymptomRecyclerView.setHasFixedSize(true)

        // botão log
        findViewById<FloatingActionButton>(R.id.btnLogChoice).setOnClickListener {
            startActivity(Intent(this, LogChoiceActivity::class.java))
        }

        // botão mês anterior
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            setupCalendar()
        }

        // botão próximo mês
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            setupCalendar()
        }

        // busca registros e setup calendário
        fetchCycleRecordsAndSetupCalendar()

        // cria canal de notificações
        NotificationHelper.createNotificationChannel(this)

        // checa permissão notificações
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) sendAllNotifications()
            else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else sendAllNotifications()
    }

    // envia notificações
    private fun sendAllNotifications() {
        NotificationHelper.sendNotification(this, "Your period is late")
        NotificationHelper.sendNotification(this, "Period in 3 days")
        NotificationHelper.sendNotification(this, "Cycle is 3-8 days, looks normal")
    }

    // busca registros do firestore
    private fun fetchCycleRecordsAndSetupCalendar() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("cycle_records")
            .get()
            .addOnSuccessListener { snapshot ->
                cachedCycleRecords = snapshot.documents.mapNotNull { doc ->
                    try { doc.toObject(CycleRecord::class.java) } catch (e: Exception) { null }
                }.sortedBy { it.date }

                // atualiza fase e calendário
                handleCurrentPhaseAndNextCycle()
                setupCalendar()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cycle records.", Toast.LENGTH_SHORT).show()
            }
    }

    // calcula fase atual e próximo ciclo
    private fun handleCurrentPhaseAndNextCycle() {
        if (cachedCycleRecords.isEmpty()) {
            currentPhaseText.text = "No cycle data yet"
            nextCycleText.text = "Log your flow to start tracking your cycle"
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val today = sdf.parse(sdf.format(Date())) ?: return
        // registros menstruais manuais
        val menstruationRecords =
            cachedCycleRecords.filter { it.phase.lowercase() == "menstruation" && it.isManual }

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

        // checa se menstruando hoje
        val isMenstruating = menstruationRecords.any { record ->
            val recordDate = sdf.parse(record.date) ?: return@any false
            val flowDuration = when (record.flow?.lowercase()) {
                "light" -> 3
                "medium" -> 5
                "heavy" -> 7
                else -> 5
            }
            val endDate = Calendar.getInstance()
                .apply { time = recordDate; add(Calendar.DAY_OF_MONTH, flowDuration - 1) }.time
            !today.before(recordDate) && !today.after(endDate)
        }

        // define fase atual
        val currentPhase =
            if (isMenstruating) "Menstruation" else resolvePhaseForDate(sdf.format(today))
        currentPhaseText.text = "You are in $currentPhase Phase"

        // busca conselhos e sintomas
        fetchAdviceForPhase(currentPhase)
        fetchFrequentSymptoms(currentPhase)

        // calcula próximo ciclo
        if (start != null) {
            val cal = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_MONTH, 28) }
            val daysLeft =
                ((cal.timeInMillis - Calendar.getInstance().timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            nextCycleText.text =
                "Next cycle in $daysLeft days, scheduled for ${
                    SimpleDateFormat(
                        "MMMM dd",
                        Locale.ENGLISH
                    ).format(cal.time)
                }"
        } else {
            nextCycleText.text = "Next cycle date unavailable"
        }
    }

    // setup calendário
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

        // espaços antes do primeiro dia
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

        // adiciona dias
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

                val bg = ContextCompat.getDrawable(context, R.drawable.rounded_background)?.mutate()
                background = bg
                bg?.setTint(dayPhaseColor)

                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))

                // destaca dia atual
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

    // resolve fase para data
    private fun resolvePhaseForDate(dateStr: String): String {
        cachedCycleRecords.find { it.date == dateStr && it.isManual }?.let { return it.phase }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = sdf.parse(dateStr) ?: return "Unknown"

        val menstruationRecords = cachedCycleRecords.filter {
            it.phase.lowercase() == "menstruation" && it.isManual && it.date <= dateStr
        }.sortedByDescending { it.date }

        if (menstruationRecords.isEmpty()) return "Unknown"

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

    // busca conselho para fase
    private fun fetchAdviceForPhase(phase: String) {
        val prefs = getSharedPreferences("advice_prefs", MODE_PRIVATE)

        db.collection("advices")
            .whereEqualTo("phase", phase.replaceFirstChar { it.uppercase() })
            .get()
            .addOnSuccessListener { result ->
                val advices =
                    result.documents.mapNotNull { it.toObject(com.example.lunaflow.models.Advice::class.java) }

                val adviceToShow = if (advices.isNotEmpty()) {
                    val lastIndex = prefs.getInt("last_advice_index_$phase", 0)
                    prefs.edit { putInt("last_advice_index_$phase", lastIndex + 1) }
                    advices[lastIndex % advices.size]
                } else com.example.lunaflow.models.Advice(phase, "No advices available")

                adviceRecyclerView.adapter = AdviceAdapter(listOf(adviceToShow))
            }
            .addOnFailureListener {
                adviceRecyclerView.adapter = AdviceAdapter(
                    listOf(
                        com.example.lunaflow.models.Advice(
                            phase,
                            "Failed to load advice."
                        )
                    )
                )
            }
    }

    // busca sintomas frequentes
    private fun fetchFrequentSymptoms(phase: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .collection("cycle_records")
            .get()
            .addOnSuccessListener { snapshot ->

                val symptomLogs = snapshot.documents.mapNotNull { doc ->
                    try {
                        val record = doc.toObject(CycleRecord::class.java)
                        record?.logs?.filter { it.type.lowercase() == "symptoms" }
                    } catch (e: Exception) {
                        null
                    }
                }.flatten()

                val symptomFrequency = mutableMapOf<String, Int>()

                symptomLogs.forEach { log ->
                    log.data.forEach { (key, value) ->
                        if (key.lowercase() != "flow" && value is Boolean && value) {
                            val symptomKey = key.lowercase()
                            symptomFrequency[symptomKey] = symptomFrequency.getOrDefault(symptomKey, 0) + 1
                        }
                    }
                }

                if (symptomFrequency.isEmpty()) {
                    frequentSymptomRecyclerView.adapter =
                        SymptomAdviceAdapter(listOf("No symptoms registered"))
                    return@addOnSuccessListener
                }

                val symptomAdvices = mutableListOf<String>()
                var loadedCount = 0
                symptomFrequency.keys.forEach { symptom ->
                    getSymptomAdvice(phase, symptom) { advice ->
                        symptomAdvices.add("Symptom: ${symptom.replaceFirstChar { it.uppercase() }}\nAdvice: $advice")
                        loadedCount++
                        if (loadedCount == symptomFrequency.size) {
                            frequentSymptomRecyclerView.adapter = SymptomAdviceAdapter(symptomAdvices)
                        }
                    }
                }
            }
            .addOnFailureListener {
                frequentSymptomRecyclerView.adapter =
                    SymptomAdviceAdapter(listOf("Failed to load symptoms."))
            }
    }

    // busca conselho de sintoma
    private fun getSymptomAdvice(phase: String, symptom: String, callback: (String) -> Unit) {
        db.collection("symptom_advices")
        db.collection("symptom_advices")
            .whereEqualTo("phase", phase.lowercase())
            .whereEqualTo("symptom", symptom.lowercase())
            .get()
            .addOnSuccessListener { result ->
                val advice = if (result.documents.isNotEmpty())
                    result.documents[0].getString("advice") ?: "No advice available."
                else "No advice available."
                callback(advice)
            }
            .addOnFailureListener { callback("No advice available.") }
    }

    // bottom sheet detalhes do dia
    private fun showDayBottomSheet(dateStr: String, record: CycleRecord?) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_day_details, null)

        val phaseText = view.findViewById<TextView>(R.id.bottomSheetPhase)
        val logsText = view.findViewById<TextView>(R.id.bottomSheetLogs)
        val symptomsText = view.findViewById<TextView>(R.id.bottomSheetSymptoms)

        if (record != null) {
            // fase
            phaseText.text = "Phase: ${record.phase}"

            val activityLogs = record.logs.filter { it.type.equals("activity", ignoreCase = true) }
                .mapNotNull { log ->
                    val activities = mutableListOf<String>()

                    log.data.forEach { (key, value) ->
                        when (value) {
                            is Boolean -> if (value) {
                                activities.add(key.replaceFirstChar { it.uppercase() })
                            }
                            is String -> if (value.isNotBlank()) {
                                activities.add("${key.replaceFirstChar { it.uppercase() }}: $value")
                            }
                        }
                    }

                    if (activities.isNotEmpty())
                        "Activity: ${activities.joinToString(", ")}"
                    else null
                }

            logsText.text =
                if (activityLogs.isNotEmpty()) activityLogs.joinToString("\n")
                else "No activity logs"

            val symptomLogs = record.logs.filter { it.type.lowercase() == "symptoms" }
            val activeSymptoms = mutableListOf<String>()
            var flowToShow: String? = null

            symptomLogs.forEach { log ->
                log.data.forEach { (key, value) ->
                    when {
                        key.equals("flow", true) && value is String -> flowToShow = value
                        value is Boolean && value -> activeSymptoms.add(key.replaceFirstChar { it.uppercase() })
                    }
                }
            }

            val symptomOutput = mutableListOf<String>()
            flowToShow?.let { symptomOutput.add("Flow: $it") }
            if (activeSymptoms.isNotEmpty()) symptomOutput.add(
                "Symptoms: ${activeSymptoms.joinToString(", ")}"
            )

            symptomsText.text =
                if (symptomOutput.isNotEmpty()) symptomOutput.joinToString("\n") else "No symptoms"
        } else {
            phaseText.text = "No record for this day"
            logsText.text = ""
            symptomsText.text = ""
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
}