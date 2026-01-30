package com.example.lunaflow.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lunaflow.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var bottomNav: BottomNavigationView
    protected lateinit var toolbar: MaterialToolbar

    private val REQUEST_NOTIFICATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        createNotificationChannel()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    protected fun setContentLayout(layoutRes: Int) {
        setContentView(R.layout.activity_base)

        toolbar = findViewById(R.id.toolbar)
        //setSupportActionBar(toolbar)
        bottomNav = findViewById(R.id.bottomNavigationView)

        val container = findViewById<FrameLayout>(R.id.container)
        layoutInflater.inflate(layoutRes, container, true)
    }

    protected fun setupBottomNav(selectedItemId: Int? = null) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_history -> {
                    if (this !is HistoryActivity) {
                        startActivity(Intent(this, HistoryActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfileActivity) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }

        bottomNav.menu.setGroupCheckable(0, true, true)

        if (selectedItemId != null) {
            bottomNav.selectedItemId = selectedItemId
        } else {
            bottomNav.menu.setGroupCheckable(0, false, true)
        }
    }

    protected fun showBottomNav(show: Boolean) {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    protected fun showToolbar(show: Boolean) {
        if (::toolbar.isInitialized) {
            toolbar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    protected fun setToolbarTitle(title: String) {
        if (::toolbar.isInitialized) {
            toolbar.title = title
        }
    }

    /** Toolbar menu **/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Notification helper **/
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LunaFlowChannel"
            val descriptionText = "Channel for LunaFlow notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("lunaFlow_channel_id", name, importance)
            channel.description = descriptionText
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(title: String, message: String) {
        // Check runtime permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão não concedida: não faz nada ou pede permissao
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )

        val builder = NotificationCompat.Builder(this, "lunaFlow_channel_id")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
