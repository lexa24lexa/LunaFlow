package com.example.lunaflow.activities

import android.os.Bundle
import com.example.lunaflow.R

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav(R.id.nav_profile)
    }
}
