package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.lunaflow.R

class LogChoiceActivity : BaseActivity() {

    // inicializa activity e define clique dos botoes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentLayout(R.layout.activity_log_choice)
        setToolbarTitle("LunaFlow")
        showToolbar(true)
        showBottomNav(true)
        setupBottomNav()

        findViewById<Button>(R.id.symptomButton).setOnClickListener {
            startActivity(Intent(this, LogSymptomActivity::class.java))
        }

        findViewById<Button>(R.id.activityButton).setOnClickListener {
            startActivity(Intent(this, LogActivityActivity::class.java))
        }
    }
}
