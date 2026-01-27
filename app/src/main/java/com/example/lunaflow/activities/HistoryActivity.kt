package com.example.lunaflow.activities

import android.os.Bundle
import com.example.lunaflow.R

class HistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        // Bottom navigation
        setToolbarTitle("History")
        setupBottomNav(R.id.nav_history)
        showToolbar(true)
        showBottomNav(true)
    }
}
