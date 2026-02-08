package com.example.lunaflow.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object NotificationHelper {

    private const val CHANNEL_ID = "lunaflow_channel"
    private var notificationId = 0
    private val auth = FirebaseAuth.getInstance()

    // creates notifications channel
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LunaFlow Notifications"
            val descriptionText = "Notifications for menstrual cycle alerts"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // local notification
    fun sendNotification(context: Context, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationHelper", "notification permission not granted")
                return
            }
        }

        // builds notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("LunaFlow Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // shows notification
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId++, builder.build())
        }

        // saves notification in firestore (only if user exists)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val notificationMap = hashMapOf(
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(currentUser.uid)
                .collection("notifications")
                .add(notificationMap)
        }
    }
}
