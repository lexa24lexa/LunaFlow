package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.LogType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class LogSymptomActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        // --- GET SYMPTOMS ---
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

        val anySymptomSelected = nausea || headache || cramps || bloating || dizziness ||
                fatigue || moodSwings || anxiety || irritability || otherSymptomsText.isNotEmpty()

        // --- GET FLOW ---
        val flowGroup = findViewById<RadioGroup>(R.id.flowRadioGroup)
        val selectedFlow = when (flowGroup.checkedRadioButtonId) {
            R.id.flowLight -> "Light"
            R.id.flowMedium -> "Medium"
            R.id.flowHeavy -> "Heavy"
            else -> null // important: null if not selected
        }

        if (!anySymptomSelected && selectedFlow == null) {
            Toast.makeText(this, "Please select at least one symptom or flow level", Toast.LENGTH_SHORT).show()
            return
        }

        val symptoms: Map<String, Boolean> = mapOf(
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

        // --- PREPARE LOGS ---
        val logsMap = hashMapOf<String, Any>()
        selectedFlow?.let { logsMap["flow"] = it } // only store flow if selected
        if (otherSymptomsText.isNotEmpty()) logsMap["otherSymptoms"] = otherSymptomsText

        // --- FETCH CYCLE RECORDS ---
        db.collection("users").document(userId).collection("cycle_records")
            .get()
            .addOnSuccessListener { snapshot ->
                val cycleRecords = snapshot.documents.mapNotNull { it.toObject(CycleRecord::class.java) }

                val (currentPhase, isManual) = if (selectedFlow != null) {
                    "Menstruation" to true
                } else {
                    resolvePhaseForDate(todayStr, cycleRecords) to false
                }

                // --- SAVE LOG ---
                val log = hashMapOf(
                    "type" to LogType.symptoms,
                    "userId" to userId,
                    "phase" to currentPhase,
                    "flow" to selectedFlow, // null if not selected
                    "timestamp" to FieldValue.serverTimestamp(),
                    "symptoms" to symptoms,
                    "otherSymptoms" to otherSymptomsText
                )
                db.collection("users").document(userId).collection("logs").add(log)

                // --- UPDATE OR CREATE CYCLE RECORD ---
                val cycleRecordRef = db.collection("users").document(userId).collection("cycle_records").document(todayStr)
                cycleRecordRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val existingLogs = snapshot.get("logs") as? Map<String, Any> ?: emptyMap()
                        val mergedLogs = existingLogs.toMutableMap()
                        selectedFlow?.let { mergedLogs["flow"] = it } // only update if selected
                        if (otherSymptomsText.isNotEmpty()) mergedLogs["otherSymptoms"] = otherSymptomsText

                        val updates = hashMapOf<String, Any>(
                            "symptoms" to symptoms,
                            "phase" to currentPhase,
                            "isManual" to true,
                            "logs" to mergedLogs
                        )
                        selectedFlow?.let { updates["flow"] = it } // root flow only if selected
                        cycleRecordRef.set(updates, SetOptions.merge())
                    } else {
                        val newRecord = hashMapOf(
                            "id" to todayStr,
                            "date" to todayStr,
                            "phase" to currentPhase,
                            "symptoms" to symptoms,
                            "logs" to logsMap,
                            "isManual" to isManual
                        )
                        selectedFlow?.let { newRecord["flow"] = it } // only add flow if selected
                        cycleRecordRef.set(newRecord)
                    }
                }

                Toast.makeText(this, "Symptoms saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch cycle data", Toast.LENGTH_SHORT).show()
            }
    }

    // --- CALCULATE PHASE BASED ON CYCLE RECORDS ---
    private fun resolvePhaseForDate(dateStr: String, records: List<CycleRecord>): String {
        records.find { it.date == dateStr }?.let { return it.phase }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = sdf.parse(dateStr) ?: return "Unknown"

        val lastManualMenstruation = records.lastOrNull {
            it.phase.lowercase() == "menstruation" && it.isManual && it.date <= dateStr
        } ?: return "Unknown"

        val start = sdf.parse(lastManualMenstruation.date) ?: return "Unknown"
        val diffDays = ((date.time - start.time) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays in 0..4 -> "Menstruation"
            diffDays in 5..13 -> "Follicular"
            diffDays in 14..15 -> "Ovulation"
            diffDays in 16..27 -> "Luteal"
            else -> "Unknown"
        }
    }
}
