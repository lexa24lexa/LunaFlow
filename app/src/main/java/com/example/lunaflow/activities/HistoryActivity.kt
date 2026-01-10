package com.example.lunaflow.activities

import android.os.Bundle
import com.example.lunaflow.R

class HistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav(R.id.nav_history)
    }
}
