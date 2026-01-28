package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.lunaflow.R

class WelcomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_welcome)

        setToolbarTitle("LunaFlow")
        showToolbar(false)
        showBottomNav(false)

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        findViewById<Button>(R.id.registerButton).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
