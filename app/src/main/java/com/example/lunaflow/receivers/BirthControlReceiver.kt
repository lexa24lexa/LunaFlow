package com.example.lunaflow.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lunaflow.utils.NotificationHelper
import com.example.lunaflow.utils.ReminderScheduler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class BirthControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // local notification
        NotificationHelper.sendNotification(context, "Take your birth control pill")

        // try to save in Firestore if user exists
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val notificationMap = hashMapOf(
                "message" to "Take your birth control pill",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(currentUser.uid)
                .collection("notifications")
                .add(notificationMap)
        }

        // reschedule next day
        val sharedPrefs = context.getSharedPreferences("birth_control_reminder", Context.MODE_PRIVATE)
        val hour = sharedPrefs.getInt("hour", -1)
        val minute = sharedPrefs.getInt("minute", -1)
        if (hour >= 0 && minute >= 0) {
            ReminderScheduler.scheduleDailyReminder(context, hour, minute)
        }
    }
}
