package com.example.lunaflow.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.LogType
import com.example.lunaflow.models.UserCycleProfile
import com.example.lunaflow.utils.PredictionEngine
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LogActivityActivity : BaseActivity() {

    private lateinit var timeSinceSpinner: Spinner
    private lateinit var otherMedSpinner: Spinner
    private val riskyMeds = mutableListOf<String>()
    private var editingLogEntry: LogEntry? = null

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

        editingLogEntry = intent.getParcelableExtra("logEntry")
        editingLogEntry?.let { prefillFields(it) }

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveActivity() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun prefillFields(log: LogEntry) {
        val data = log.data

        findViewById<CheckBox>(R.id.sex).isChecked = data["sex"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.masturbation).isChecked = data["masturbation"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.planBPill).isChecked = data["planBPill"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.birthControl).isChecked = data["birthControl"] as? Boolean ?: false
        val hasOtherMed = (data["otherMedication"] as? String)?.isNotEmpty() ?: false
        findViewById<CheckBox>(R.id.otherMedication).isChecked = hasOtherMed

        findViewById<LinearLayout>(R.id.sexOptionsLayout).visibility =
            if (findViewById<CheckBox>(R.id.sex).isChecked) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.planBOptionsLayout).visibility =
            if (findViewById<CheckBox>(R.id.planBPill).isChecked) View.VISIBLE else View.GONE
        otherMedSpinner.visibility = if (hasOtherMed) View.VISIBLE else View.GONE

        findViewById<EditText>(R.id.sexDetails).setText(data["sexDetails"] as? String ?: "")
        findViewById<EditText>(R.id.planBDetails).setText(data["planBDetails"] as? String ?: "")

        val protection = data["protectionUsed"] as? String
        findViewById<RadioButton>(R.id.usedProtectionYes).isChecked = protection == "Yes"
        findViewById<RadioButton>(R.id.usedProtectionNo).isChecked = protection != "Yes"

        val otherMed = data["otherMedication"] as? String
        if (!otherMed.isNullOrEmpty() && riskyMeds.isNotEmpty()) {
            val index = riskyMeds.indexOf(otherMed)
            if (index >= 0) otherMedSpinner.setSelection(index)
        }
    }

    private fun fetchMedicationsFromFirebase() {
        FirebaseFirestore.getInstance().collection("medications").get()
            .addOnSuccessListener { docs ->
                riskyMeds.clear()
                for (doc in docs) doc.getString("name")?.let { riskyMeds.add(it) }
                val medAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, riskyMeds)
                medAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                otherMedSpinner.adapter = medAdapter

                editingLogEntry?.let { log ->
                    val otherMed = log.data["otherMedication"] as? String
                    if (!otherMed.isNullOrEmpty()) {
                        val index = riskyMeds.indexOf(otherMed)
                        if (index >= 0) otherMedSpinner.setSelection(index)
                    }
                }
            }
    }

    private fun saveActivity() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        val selectedDate = intent.getStringExtra("date")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        val sexChecked = findViewById<CheckBox>(R.id.sex).isChecked
        val planBChecked = findViewById<CheckBox>(R.id.planBPill).isChecked
        val birthControlChecked = findViewById<CheckBox>(R.id.birthControl).isChecked
        val otherMedChecked = findViewById<CheckBox>(R.id.otherMedication).isChecked
        val selectedOtherMed = if (otherMedChecked && otherMedSpinner.adapter.count > 0)
            otherMedSpinner.selectedItem.toString() else null

        if (birthControlChecked && sexChecked && !selectedOtherMed.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage(
                    "The medication '$selectedOtherMed' can interfere with your birth control. " +
                            "Please take precautions to avoid unwanted pregnancies."
                )
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        val activityData = mutableMapOf<String, Any>(
            "sex" to sexChecked,
            "masturbation" to findViewById<CheckBox>(R.id.masturbation).isChecked,
            "planBPill" to planBChecked,
            "birthControl" to birthControlChecked,
            "otherMedication" to selectedOtherMed.orEmpty()
        )

        if (sexChecked) {
            activityData["protectionUsed"] =
                if (findViewById<RadioButton>(R.id.usedProtectionYes).isChecked) "Yes" else "No"
            activityData["sexDetails"] = findViewById<EditText>(R.id.sexDetails).text.toString().trim()
        }

        if (planBChecked) {
            activityData["planBDetails"] = findViewById<EditText>(R.id.planBDetails).text.toString().trim()
        }

        val log = editingLogEntry?.copy(
            timestamp = System.currentTimeMillis(),
            data = activityData,
            date = selectedDate
        ) ?: LogEntry(
            id = UUID.randomUUID().toString(),
            type = LogType.activity,
            timestamp = System.currentTimeMillis(),
            title = "Activity",
            details = "",
            data = activityData,
            date = selectedDate
        )

        val logsCollection = db.collection("users").document(userId).collection("logs")
        if (editingLogEntry != null) logsCollection.document(editingLogEntry!!.id).set(log)
        else logsCollection.add(log)

        Toast.makeText(this, if (editingLogEntry != null) "Activity updated" else "Activity saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
