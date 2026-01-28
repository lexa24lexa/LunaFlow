package com.example.lunaflow.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LogActivityActivity : AppCompatActivity() {

    private lateinit var timeSinceSpinner: Spinner
    private lateinit var otherMedSpinner: Spinner
    private val riskyMeds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_activity)

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
        sexCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sexLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        planBCheckBox.setOnCheckedChangeListener { _, isChecked ->
            planBLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        otherMedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            otherMedSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // fetch medications from firebase
        fetchMedicationsFromFirebase()

        // save/cancel buttons
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveActivity() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun fetchMedicationsFromFirebase() {
        FirebaseFirestore.getInstance().collection("medications")
            .get()
            .addOnSuccessListener { docs ->
                riskyMeds.clear()
                for (doc in docs) {
                    doc.getString("name")?.let { riskyMeds.add(it) }
                }
                val medAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, riskyMeds)
                medAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                otherMedSpinner.adapter = medAdapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load medications", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveActivity() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sexChecked = findViewById<CheckBox>(R.id.sex).isChecked
        val planBChecked = findViewById<CheckBox>(R.id.planBPill).isChecked
        val otherMedChecked = findViewById<CheckBox>(R.id.otherMedication).isChecked

        // validate sex & plan B
        if (sexChecked) {
            val protectionYes = findViewById<RadioButton>(R.id.usedProtectionYes).isChecked
            val protectionNo = findViewById<RadioButton>(R.id.usedProtectionNo).isChecked
            if (!protectionYes && !protectionNo) {
                Toast.makeText(this, "Please select if protection was used", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (planBChecked) {
            val pillLevon = findViewById<RadioButton>(R.id.levonorgestrel).isChecked
            val pillLipristal = findViewById<RadioButton>(R.id.lipristal).isChecked
            if (!pillLevon && !pillLipristal) {
                Toast.makeText(this, "Please select Plan B pill type", Toast.LENGTH_SHORT).show()
                return
            }

            if (timeSinceSpinner.selectedItem == null) {
                Toast.makeText(this, "Please select time since intercourse", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // activity data
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

        if (planBChecked) {
            val pillType = if (findViewById<RadioButton>(R.id.levonorgestrel).isChecked) "Levonorgestrel" else "Lipristal"
            activityData["pillType"] = pillType
            activityData["timeSinceIntercourse"] = timeSinceSpinner.selectedItem.toString()
            activityData["planBDetails"] = findViewById<EditText>(R.id.planBDetails).text.toString().trim()
        }

        if (otherMedChecked && otherMedSpinner.selectedItem != null) {
            val selectedMed = otherMedSpinner.selectedItem.toString()
            activityData["otherMedication"] = selectedMed

            // alert if risky + birth control + unprotected sex
            val sexNoProtection = sexChecked && findViewById<RadioButton>(R.id.usedProtectionNo).isChecked
            if (findViewById<CheckBox>(R.id.birthControl).isChecked && sexNoProtection && riskyMeds.contains(selectedMed)) {
                Toast.makeText(this, "Warning: This medication may reduce birth control effectiveness!", Toast.LENGTH_LONG).show()
            }
        }

        // save information to firestore
        val log = hashMapOf(
            "type" to "activity",
            "timestamp" to FieldValue.serverTimestamp(),
            "activity" to activityData
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("logs")
            .add(log)
            .addOnSuccessListener {
                Toast.makeText(this, "Activity saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving activity", Toast.LENGTH_SHORT).show()
            }
    }
}
