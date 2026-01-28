package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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

        val anySymptomSelected = nausea || headache || cramps || bloating || dizziness || fatigue ||
                moodSwings || anxiety || irritability || otherSymptomsText.isNotEmpty()

        val flowGroup = findViewById<RadioGroup>(R.id.flowRadioGroup)
        val flowSelected = flowGroup.checkedRadioButtonId != -1

        if (!anySymptomSelected && !flowSelected) {
            Toast.makeText(this, "Please select at least one symptom or flow level", Toast.LENGTH_SHORT).show()
            return
        }

        val symptoms = hashMapOf(
            "nausea" to nausea,
            "headache" to headache,
            "cramps" to cramps,
            "bloating" to bloating,
            "dizziness" to dizziness,
            "fatigue" to fatigue,
            "moodSwings" to moodSwings,
            "anxiety" to anxiety,
            "irritability" to irritability,
            "otherSymptoms" to otherSymptomsText
        )

        val flow = when (flowGroup.checkedRadioButtonId) {
            R.id.flowLight -> "Light"
            R.id.flowMedium -> "Medium"
            R.id.flowHeavy -> "Heavy"
            else -> "None"
        }

        val log = hashMapOf(
            "type" to "symptoms",
            "timestamp" to FieldValue.serverTimestamp(),
            "flow" to flow,
            "symptoms" to symptoms
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("logs")
            .add(log)
            .addOnSuccessListener {
                Toast.makeText(this, "Symptoms saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving symptoms", Toast.LENGTH_SHORT).show()
            }
    }
}
