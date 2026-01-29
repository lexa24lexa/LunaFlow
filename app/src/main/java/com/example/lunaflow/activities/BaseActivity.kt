package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.lunaflow.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var bottomNav: BottomNavigationView
    protected lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    /**
     * Call this instead of setContentView for screens.
     * Inflates the layout inside the base container.
     */
    protected fun setContentLayout(layoutRes: Int) {
        // Inflate base layout
        setContentView(R.layout.activity_base)

        // Initialize toolbar and bottom nav
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigationView)

        // Inflate the child layout inside the container
        val container = findViewById<FrameLayout>(R.id.container)
        layoutInflater.inflate(layoutRes, container, true)
    }

    /**
     * Setup bottom navigation with correct selected item.
     */
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

    /**
     * Show or hide bottom navigation
     */
    protected fun showBottomNav(show: Boolean) {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    /**
     * Show or hide toolbar
     */
    protected fun showToolbar(show: Boolean) {
        if (::toolbar.isInitialized) {
            toolbar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    /**
     * Set toolbar title dynamically
     */
    protected fun setToolbarTitle(title: String) {
        if (::toolbar.isInitialized) {
            toolbar.title = title
        }
    }
}
