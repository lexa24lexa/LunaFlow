package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.lunaflow.R
import com.example.lunaflow.models.UserCycleProfile
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : BaseActivity() {

    private var userProfile = UserCycleProfile() // default

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

        loadUserProfile()

        setupUserInfo()
        setupDarkModeSwitch()
        setupLogoutButton()
        setupCycleInfoEdit()
    }

    private fun loadUserProfile() {
        // Load saved profile (SharedPreferences or Firestore)
        val prefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        userProfile = UserCycleProfile(
            lastPeriodStart = prefs.getString("lastPeriodStart", "2026-02-01") ?: "2026-02-01",
            cycleLength = prefs.getInt("cycleLength", 28),
            periodLength = prefs.getInt("periodLength", 5)
        )
    }

    private fun saveUserProfile() {
        val prefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        prefs.edit().apply {
            putString("lastPeriodStart", userProfile.lastPeriodStart)
            putInt("cycleLength", userProfile.cycleLength)
            putInt("periodLength", userProfile.periodLength)
            apply()
        }
    }

    private fun setupUserInfo() {
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
    }

    private fun setupDarkModeSwitch() {
        val darkModeSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        val prefs = getSharedPreferences("dark_mode", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode
        setDarkMode(isDarkMode)

        darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            setDarkMode(checked)
            prefs.edit().putBoolean("dark_mode", checked).apply()
        }
    }

    private fun setDarkMode(enabled: Boolean) {
        if (enabled) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun setupLogoutButton() {
        findViewById<TextView>(R.id.logoutButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupCycleInfoEdit() {
        val editButton = findViewById<ImageView>(R.id.btnEditCycleInfo)
        editButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_cycle_info, null)
            val lastPeriodInput = dialogView.findViewById<EditText>(R.id.lastPeriodInput)
            val cycleLengthInput = dialogView.findViewById<EditText>(R.id.cycleLengthInput)
            val periodLengthInput = dialogView.findViewById<EditText>(R.id.periodLengthInput)

            lastPeriodInput.setText(userProfile.lastPeriodStart)
            cycleLengthInput.setText(userProfile.cycleLength.toString())
            periodLengthInput.setText(userProfile.periodLength.toString())

            AlertDialog.Builder(this)
                .setTitle("Edit Cycle Info")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    userProfile.lastPeriodStart = lastPeriodInput.text.toString()
                    userProfile.cycleLength = cycleLengthInput.text.toString().toIntOrNull() ?: 28
                    userProfile.periodLength = periodLengthInput.text.toString().toIntOrNull() ?: 5
                    saveUserProfile()
                    Toast.makeText(this, "Cycle info updated", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
