package com.example.lunaflow.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.lunaflow.R
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

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

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // USER INFO CARD
        val user = FirebaseAuth.getInstance().currentUser
        val userName = findViewById<TextView>(R.id.userName)
        val userEmail = findViewById<TextView>(R.id.userEmail)
        val userAvatar = findViewById<ImageView>(R.id.userAvatar)

        userName.text = user?.displayName ?: "No Name"
        userEmail.text = user?.email ?: "No Email"

        val photoUrl = user?.photoUrl
        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).circleCrop().into(userAvatar)
        } else {
            userAvatar.setImageResource(R.drawable.ic_user_placeholder)
        }

        // LOGOUT BUTTON
        val logoutButton = findViewById<TextView>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        // DARK MODE SWITCH
        val darkModeSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode
        setDarkMode(isDarkMode)

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            setDarkMode(isChecked)
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
        }
    }

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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setDarkMode(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
