package com.example.lunaflow.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LogSymptomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_symptom)

        val saveButton: Button = findViewById(R.id.saveButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)

        saveButton.setOnClickListener {
            saveSymptoms()
        }

        cancelButton.setOnClickListener { finish() }
    }

    private fun saveSymptoms() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // collect symptom values
        val nausea = findViewById<CheckBox>(R.id.nausea).isChecked
        val headache = findViewById<CheckBox>(R.id.headache).isChecked
        val cramps = findViewById<CheckBox>(R.id.cramps).isChecked
        val bloating = findViewById<CheckBox>(R.id.bloating).isChecked
        val dizziness = findViewById<CheckBox>(R.id.dizziness).isChecked
        val fatigue = findViewById<CheckBox>(R.id.fatigue).isChecked
        val moodSwings = findViewById<CheckBox>(R.id.moodSwings).isChecked
        val anxiety = findViewById<CheckBox>(R.id.anxiety).isChecked
        val irritability = findViewById<CheckBox>(R.id.irritability).isChecked
        val otherSymptomsText = findViewById<EditText>(R.id.otherSymptoms).text.toString().trim()

        // check if at least one symptom or otherSymptoms is filled
        val anySymptomSelected = nausea || headache || cramps || bloating || dizziness || fatigue ||
                moodSwings || anxiety || irritability || otherSymptomsText.isNotEmpty()

        // flow
        val flowGroup = findViewById<RadioGroup>(R.id.flowRadioGroup)
        val flowSelected = flowGroup.checkedRadioButtonId != -1

        if (!anySymptomSelected && !flowSelected) {
            Toast.makeText(this, "Please select at least one symptom or flow level", Toast.LENGTH_SHORT).show()
            return
        }

        // symptoms data
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

        // flow
        val flow = when (flowGroup.checkedRadioButtonId) {
            R.id.flowLight -> "Light"
            R.id.flowMedium -> "Medium"
            R.id.flowHeavy -> "Heavy"
            else -> "None"
        }

        // save information to firestore
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
