package com.example.lunaflow.activities

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.lunaflow.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

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

        // time since intercourse spinner
        timeSinceSpinner = findViewById(R.id.timeSinceIntercourseSpinner)
        val timeOptions = listOf("30 min ago", "1 hour ago", "2 hours ago", "6 hours ago", "12 hours ago", "24 hours ago")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSinceSpinner.adapter = adapter

        // show/hide layouts
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
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        val sexChecked = findViewById<CheckBox>(R.id.sex).isChecked
        val planBChecked = findViewById<CheckBox>(R.id.planBPill).isChecked
        val otherMedChecked = findViewById<CheckBox>(R.id.otherMedication).isChecked

        val activityData: MutableMap<String, Any> = hashMapOf(
            "sex" to sexChecked,
            "masturbation" to findViewById<CheckBox>(R.id.masturbation).isChecked,
            "planBPill" to planBChecked,
            "birthControl" to findViewById<CheckBox>(R.id.birthControl).isChecked,
            "otherMedication" to ""
        )

        if (sexChecked) {
            val protection = if (findViewById<RadioButton>(R.id.usedProtectionYes).isChecked) "Yes" else "No"
            activityData["protectionUsed"] = protection
            activityData["sexDetails"] = findViewById<EditText>(R.id.sexDetails).text.toString().trim()
        }

        if (planBChecked && otherMedChecked && otherMedSpinner.adapter != null && otherMedSpinner.adapter.count > 0) {
            activityData["otherMedication"] = otherMedSpinner.selectedItem.toString()
        }

        // Salvar log de atividade
        val log = hashMapOf(
            "type" to "activity",
            "userId" to userId,
            "timestamp" to FieldValue.serverTimestamp(),
            "activity" to activityData
        )
        db.collection("users").document(userId).collection("logs").add(log)

        // Atualizar ou criar CycleRecord
        val cycleRecordRef = db.collection("users").document(userId).collection("cycle_records").document(todayStr)
        cycleRecordRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any>(
                    "logs" to activityData
                )
                cycleRecordRef.update(updates)
            } else {
                val newRecord = hashMapOf(
                    "date" to todayStr,
                    "phase" to "luteal", // futuramente calculável
                    "logs" to activityData,
                    "symptoms" to mapOf<String, Boolean>(),
                    "flow" to "None"
                )
                cycleRecordRef.set(newRecord)
            }
        }

        Toast.makeText(this, "Activity saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
