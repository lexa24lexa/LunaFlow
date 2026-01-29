package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.lunaflow.R
import com.example.lunaflow.models.LogType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

        val flowGroup = findViewById<RadioGroup>(R.id.flowRadioGroup)
        val flow = when (flowGroup.checkedRadioButtonId) {
            R.id.flowLight -> "Light"
            R.id.flowMedium -> "Medium"
            R.id.flowHeavy -> "Heavy"
            else -> "None"
        }

        if (!anySymptomSelected && flow == "None") {
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

        val log = hashMapOf(
            "type" to LogType.symptoms,
            "userId" to userId,
            "phase" to "luteal", // futuramente calculável
            "flow" to flow,
            "timestamp" to FieldValue.serverTimestamp(),
            "symptoms" to symptoms,
            "otherSymptoms" to otherSymptomsText
        )

        // Salvar log de sintomas
        db.collection("users").document(userId).collection("logs").add(log)

        // Atualizar ou criar CycleRecord
        val cycleRecordRef = db.collection("users").document(userId).collection("cycle_records").document(todayStr)
        cycleRecordRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any>(
                    "symptoms" to symptoms,
                    "flow" to flow
                )
                cycleRecordRef.update(updates)
            } else {
                val newRecord = hashMapOf(
                    "date" to todayStr,
                    "phase" to "luteal", // futuramente calculável
                    "symptoms" to symptoms,
                    "logs" to mapOf<String, Any>(),
                    "flow" to flow
                )
                cycleRecordRef.set(newRecord)
            }
        }

        Toast.makeText(this, "Symptoms saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
