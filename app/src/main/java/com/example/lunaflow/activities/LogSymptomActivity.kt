package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.LogType
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LogSymptomActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentLayout(R.layout.activity_log_symptom)
        setToolbarTitle("LunaFlow")
        showToolbar(true)
        showBottomNav(true)
        setupBottomNav()

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveSymptoms() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun saveSymptoms() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        // Collect symptoms
        val nausea = findViewById<CheckBox>(R.id.nausea).isChecked
        val headache = findViewById<CheckBox>(R.id.headache).isChecked
        val cramps = findViewById<CheckBox>(R.id.cramps).isChecked
        val bloating = findViewById<CheckBox>(R.id.bloating).isChecked
        val dizziness = findViewById<CheckBox>(R.id.dizziness).isChecked
        val fatigue = findViewById<CheckBox>(R.id.fatigue).isChecked
        val moodSwings = findViewById<CheckBox>(R.id.moodSwings).isChecked
        val anxiety = findViewById<CheckBox>(R.id.anxiety).isChecked
        val irritability = findViewById<CheckBox>(R.id.irritability).isChecked
        val otherSymptomsText = findViewById<EditText>(R.id.otherSymptoms).text?.toString()?.trim() ?: ""

        val flowGroup = findViewById<RadioGroup>(R.id.flowRadioGroup)
        val selectedFlow = when (flowGroup.checkedRadioButtonId) {
            R.id.flowLight -> "Light"
            R.id.flowMedium -> "Medium"
            R.id.flowHeavy -> "Heavy"
            else -> null
        }

        if (!listOf(nausea, headache, cramps, bloating, dizziness, fatigue, moodSwings, anxiety, irritability)
                .any { it } && otherSymptomsText.isEmpty() && selectedFlow == null) {
            Toast.makeText(this, "Please select at least one symptom or flow level", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert to Firestore-safe Map<String, Any?>
        val symptomsMap: MutableMap<String, Any> = mutableMapOf(
            "nausea" to nausea,
            "headache" to headache,
            "cramps" to cramps,
            "bloating" to bloating,
            "dizziness" to dizziness,
            "fatigue" to fatigue,
            "moodSwings" to moodSwings,
            "anxiety" to anxiety,
            "irritability" to irritability
        )
        if (otherSymptomsText.isNotEmpty()) {
            symptomsMap["otherSymptoms"] = otherSymptomsText
        }

        val logEntry = LogEntry(
            id = UUID.randomUUID().toString(),
            type = LogType.symptoms,
            timestamp = System.currentTimeMillis(),
            title = "Symptoms",
            details = "",
            data = symptomsMap
        )

        // 1️⃣ Save log entry in logs subcollection (like Activity does)
        db.collection("users").document(userId).collection("logs")
            .add(logEntry)
            .addOnSuccessListener {
                // 2️⃣ Update or create today's cycle record
                val cycleRef = db.collection("users").document(userId)
                    .collection("cycle_records").document(todayStr)

                cycleRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val record = snapshot.toObject(CycleRecord::class.java)
                        val updatedLogs = record?.logs?.toMutableList() ?: mutableListOf()
                        updatedLogs.add(logEntry)

                        val updates: MutableMap<String, Any?> = mutableMapOf(
                            "logs" to updatedLogs,
                            "phase" to resolvePhaseForDate(record, selectedFlow),
                            "isManual" to true
                        )
                        selectedFlow?.let { updates["flow"] = it }
                        cycleRef.update(updates)
                    } else {
                        val newRecord = CycleRecord(
                            id = todayStr,
                            date = todayStr,
                            phase = if (selectedFlow != null) "Menstruation" else "Unknown",
                            flow = selectedFlow ?: "None",
                            logs = listOf(logEntry),
                            isManual = true
                        )
                        cycleRef.set(newRecord)
                    }
                }

                Toast.makeText(this, "Symptoms saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save symptoms", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resolvePhaseForDate(record: CycleRecord?, selectedFlow: String?): String {
        return if (selectedFlow != null) "Menstruation"
        else record?.phase ?: "Unknown"
    }
}
