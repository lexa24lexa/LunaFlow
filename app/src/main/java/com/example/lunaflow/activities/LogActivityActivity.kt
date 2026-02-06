package com.example.lunaflow.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.LogType
import com.example.lunaflow.models.CycleRecord
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LogActivityActivity : BaseActivity() {

    private lateinit var timeSinceSpinner: Spinner
    private lateinit var otherMedSpinner: Spinner
    private val riskyMeds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_log_activity)

        setToolbarTitle("LunaFlow")
        showToolbar(true)
        showBottomNav(true)
        setupBottomNav()

        val sexLayout: LinearLayout = findViewById(R.id.sexOptionsLayout)
        val planBLayout: LinearLayout = findViewById(R.id.planBOptionsLayout)
        val sexCheckBox: CheckBox = findViewById(R.id.sex)
        val planBCheckBox: CheckBox = findViewById(R.id.planBPill)
        val otherMedCheckBox: CheckBox = findViewById(R.id.otherMedication)
        otherMedSpinner = findViewById(R.id.otherMedicationSpinner)

        timeSinceSpinner = findViewById(R.id.timeSinceIntercourseSpinner)
        val timeOptions = listOf("30 min ago", "1 hour ago", "2 hours ago", "6 hours ago", "12 hours ago", "24 hours ago")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSinceSpinner.adapter = adapter

        sexCheckBox.setOnCheckedChangeListener { _, isChecked -> sexLayout.visibility = if (isChecked) View.VISIBLE else View.GONE }
        planBCheckBox.setOnCheckedChangeListener { _, isChecked -> planBLayout.visibility = if (isChecked) View.VISIBLE else View.GONE }
        otherMedCheckBox.setOnCheckedChangeListener { _, isChecked -> otherMedSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE }

        fetchMedicationsFromFirebase()

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveActivity() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun fetchMedicationsFromFirebase() {
        FirebaseFirestore.getInstance().collection("medications").get()
            .addOnSuccessListener { docs ->
                riskyMeds.clear()
                for (doc in docs) {
                    doc.getString("name")?.let { riskyMeds.add(it) }
                }
                val medAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, riskyMeds)
                medAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                otherMedSpinner.adapter = medAdapter
            }
    }

    private fun saveActivity() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        val sexChecked = findViewById<CheckBox>(R.id.sex).isChecked
        val planBChecked = findViewById<CheckBox>(R.id.planBPill).isChecked
        val otherMedChecked = findViewById<CheckBox>(R.id.otherMedication).isChecked

        val activityData = mutableMapOf<String, Any>(
            "sex" to sexChecked,
            "masturbation" to findViewById<CheckBox>(R.id.masturbation).isChecked,
            "planBPill" to planBChecked,
            "birthControl" to findViewById<CheckBox>(R.id.birthControl).isChecked,
            "otherMedication" to if (otherMedChecked && otherMedSpinner.adapter.count > 0)
                otherMedSpinner.selectedItem.toString() else ""
        )

        if (sexChecked) {
            val protection = if (findViewById<RadioButton>(R.id.usedProtectionYes).isChecked) "Yes" else "No"
            activityData["protectionUsed"] = protection
            activityData["sexDetails"] = findViewById<EditText>(R.id.sexDetails).text.toString().trim()
        }

        val logEntry = LogEntry(
            id = UUID.randomUUID().toString(), // unique ID to avoid Firebase crash
            type = LogType.activity,
            timestamp = System.currentTimeMillis(),
            title = "Activity",
            details = "",
            data = activityData
        )

        // save in logs subcollection
        db.collection("users").document(userId).collection("logs")
            .add(logEntry)
            .addOnSuccessListener {
                updateCycleRecord(userId, todayStr, logEntry)
                Toast.makeText(this, "Activity saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save activity", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCycleRecord(userId: String, dateStr: String, logEntry: LogEntry) {
        val cycleRecordRef = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("cycle_records").document(dateStr)

        cycleRecordRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val record = snapshot.toObject(CycleRecord::class.java)
                val updatedLogs = record?.logs?.toMutableList() ?: mutableListOf()
                updatedLogs.add(logEntry)
                cycleRecordRef.update("logs", updatedLogs)
            } else {
                val newRecord = CycleRecord(
                    id = dateStr,
                    date = dateStr,
                    phase = "luteal",
                    flow = "None",
                    logs = listOf(logEntry),
                    isManual = true
                )
                cycleRecordRef.set(newRecord)
            }
        }
    }
}
