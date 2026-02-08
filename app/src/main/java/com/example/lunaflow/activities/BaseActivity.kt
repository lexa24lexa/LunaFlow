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
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var bottomNav: BottomNavigationView
    protected lateinit var toolbar: MaterialToolbar

    private val REQUEST_NOTIFICATION_PERMISSION = 1

    // inicializa firebase e notificacoes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        createNotificationChannel()
        checkNotificationPermission()
    }

    // verifica permissao de notificacoes (android 13+)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    // define layout e inicializa toolbar e bottom nav
    protected fun setContentLayout(layoutRes: Int) {
        setContentView(R.layout.activity_base)

        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigationView)

        val container = findViewById<FrameLayout>(R.id.container)
        layoutInflater.inflate(layoutRes, container, true)

        setupBottomNav()
        selectCurrentNavItem()

        showToolbar(true)
        showBottomNav(true)
    }

    // configura bottom navigation
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

    // mostra ou esconde bottom nav
    protected fun showBottomNav(show: Boolean) {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    protected fun selectCurrentNavItem() {

        when (this) {
            is MainActivity ->
                bottomNav.selectedItemId = R.id.nav_home

            is HistoryActivity ->
                bottomNav.selectedItemId = R.id.nav_history

            is ProfileActivity ->
                bottomNav.selectedItemId = R.id.nav_profile
        }
    }

    // mostra ou esconde toolbar
    protected fun showToolbar(show: Boolean) {
        if (::toolbar.isInitialized) {
            toolbar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    // define titulo da toolbar
    protected fun setToolbarTitle(title: String) {
        if (::toolbar.isInitialized) {
            toolbar.title = title
        }
    }

    // infla menu da toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        loadNotificationCount(menu)
        return true
    }

    // trata clique nos itens do menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // cria canal de notificacoes (android 8+)
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

    // envia notificacao com titulo e mensagem
    fun sendNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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

    private fun loadNotificationCount(menu: Menu) {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(currentUser.uid)
            .collection("notifications")
            .get()
            .addOnSuccessListener { result ->

                val count = result.size()
                val item = menu.findItem(R.id.action_notifications)

                if (count > 0) {
                    item.title = "Notifications ($count)"
                }
            }
    }
}
