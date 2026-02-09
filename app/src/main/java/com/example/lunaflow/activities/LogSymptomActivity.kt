package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.LogType
import com.example.lunaflow.models.UserCycleProfile
import com.example.lunaflow.utils.PredictionEngine
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LogSymptomActivity : BaseActivity() {

    private var editingLogEntry: LogEntry? = null

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

        editingLogEntry = intent.getParcelableExtra("logEntry")

        setupFlowSpinner()
        editingLogEntry?.let { prefillFields(it) }

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveSymptoms() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun setupFlowSpinner() {
        val flowSpinner = findViewById<Spinner>(R.id.flowSpinner)
        val flowOptions = listOf("None", "Light", "Medium", "Heavy")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, flowOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        flowSpinner.adapter = adapter
    }

    private fun prefillFields(log: LogEntry) {
        val data = log.data

        findViewById<CheckBox>(R.id.nausea).isChecked = (data["nausea"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.headache).isChecked = (data["headache"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.cramps).isChecked = (data["cramps"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.bloating).isChecked = (data["bloating"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.dizziness).isChecked = (data["dizziness"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.fatigue).isChecked = (data["fatigue"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.moodSwings).isChecked = (data["moodSwings"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.anxiety).isChecked = (data["anxiety"] as? Boolean) ?: false
        findViewById<CheckBox>(R.id.irritability).isChecked = (data["irritability"] as? Boolean) ?: false

        findViewById<EditText>(R.id.otherSymptoms).setText(data["otherSymptoms"] as? String ?: "")

        val flowSpinner = findViewById<Spinner>(R.id.flowSpinner)
        val flowValue = data["flow"] as? String
        if (flowValue != null) {
            val position = (flowSpinner.adapter as ArrayAdapter<String>).getPosition(flowValue)
            if (position >= 0) flowSpinner.setSelection(position)
        }
    }

    private fun saveSymptoms() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        val selectedDate = intent.getStringExtra("date")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

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

        val flowSpinner = findViewById<Spinner>(R.id.flowSpinner)
        val selectedFlow = flowSpinner.selectedItem.toString().takeIf { it != "None" }

        if (!listOf(nausea, headache, cramps, bloating, dizziness, fatigue, moodSwings, anxiety, irritability)
                .any { it } && otherSymptomsText.isEmpty() && selectedFlow == null) {
            Toast.makeText(this, "Please select at least one symptom or flow level", Toast.LENGTH_SHORT).show()
            return
        }

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
        if (otherSymptomsText.isNotEmpty()) symptomsMap["otherSymptoms"] = otherSymptomsText
        selectedFlow?.let { symptomsMap["flow"] = it }

        val logEntry = editingLogEntry?.copy(
            timestamp = System.currentTimeMillis(),
            data = symptomsMap,
            date = selectedDate
        ) ?: LogEntry(
            id = UUID.randomUUID().toString(),
            type = LogType.symptoms,
            timestamp = System.currentTimeMillis(),
            title = "Symptoms",
            details = "",
            data = symptomsMap,
            date = selectedDate
        )

        val logsCollection = db.collection("users").document(userId).collection("logs")
        if (editingLogEntry != null) {
            logsCollection.document(editingLogEntry!!.id).set(logEntry)
        } else {
            logsCollection.add(logEntry)
        }

        Toast.makeText(this, if (editingLogEntry != null) "Symptoms updated" else "Symptoms saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
