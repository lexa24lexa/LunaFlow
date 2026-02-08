package com.example.lunaflow.activities

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.lunaflow.R
import com.example.lunaflow.utils.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ProfileActivity : BaseActivity() {

    // inicializa activity e mostra perfil
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentLayout(R.layout.activity_profile)
        setToolbarTitle("LunaFlow")
        setupBottomNav(R.id.nav_profile)
        showToolbar(true)
        showBottomNav(true)

        setupLogout()
        setupBirthControlReminder()
    }

    // logout
    private fun setupLogout() {
        val logoutButton = findViewById<TextView>(R.id.logoutButton)
        logoutButton.setOnClickListener { showLogoutConfirmation() }
    }

    // birth control reminder
    private fun setupBirthControlReminder() {

        val reminderSwitch =
            findViewById<Switch>(R.id.switchBirthControlReminder)

        val reminderTimeText =
            findViewById<TextView>(R.id.textReminderTime)

        loadReminderSettings(reminderSwitch, reminderTimeText)

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showTimePicker(reminderTimeText)
            } else {
                saveReminder(false, -1, -1)
                // also clear saved time in SharedPreferences
                val prefs = getSharedPreferences("birth_control_reminder", MODE_PRIVATE).edit()
                prefs.remove("hour")
                prefs.remove("minute")
                prefs.apply()
            }
        }
    }

    // logout logic
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // reminder logic
    private fun showTimePicker(timeText: TextView) {

        val calendar = Calendar.getInstance()

        val dialog = TimePickerDialog(
            this,
            { _, hour, minute ->

                timeText.text =
                    "Reminder time: %02d:%02d".format(hour, minute)

                saveReminder(true, hour, minute)

                // save reminder time locally for BroadcastReceiver
                val prefs = getSharedPreferences("birth_control_reminder", MODE_PRIVATE).edit()
                prefs.putInt("hour", hour)
                prefs.putInt("minute", minute)
                prefs.apply()

                ReminderScheduler.scheduleDailyReminder(
                    this,
                    hour,
                    minute
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        dialog.show()
    }

    private fun saveReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val currentUser = auth.currentUser ?: return

        val db = FirebaseFirestore.getInstance()
        val data = mapOf(
            "birthControlReminder" to enabled,
            "reminderHour" to hour,
            "reminderMinute" to minute
        )

        // Use set with merge to create the document if it doesn't exist
        db.collection("users")
            .document(currentUser.uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                if (enabled && hour >= 0 && minute >= 0) {
                    ReminderScheduler.scheduleDailyReminder(this, hour, minute)
                } else {
                    ReminderScheduler.cancelReminder(this)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ProfileActivity", "Failed to save reminder", e)
            }
    }

    private fun loadReminderSettings(
        switch: Switch,
        timeText: TextView
    ) {

        val currentUser = auth.currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->

                val enabled =
                    doc.getBoolean("birthControlReminder") ?: false

                val hour =
                    doc.getLong("reminderHour")?.toInt() ?: -1

                val minute =
                    doc.getLong("reminderMinute")?.toInt() ?: -1

                switch.isChecked = enabled

                if (hour >= 0 && minute >= 0) {
                    timeText.text =
                        "Reminder time: %02d:%02d".format(hour, minute)
                }
            }
    }
}
