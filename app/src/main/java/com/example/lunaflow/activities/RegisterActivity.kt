package com.example.lunaflow.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.Toast
import com.example.lunaflow.activities.BaseActivity
import com.example.lunaflow.R
import com.example.lunaflow.firebase.FirestoreClass
import com.example.lunaflow.models.User
import com.example.lunaflow.utils.PasswordValidator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.edit

class RegisterActivity : BaseActivity() {

    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val nameEditText = findViewById<TextInputEditText>(R.id.nameEditText)
        val surnameEditText = findViewById<TextInputEditText>(R.id.surnameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        passwordLayout = findViewById(R.id.passwordLayout)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                passwordLayout.error = if (!PasswordValidator.isValid(password)) {
                    "Min 8 chars, at least 1 letter & 1 number"
                } else null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            when {
                name.isEmpty() || surname.isEmpty() || email.isEmpty() ||
                        password.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }

                password != confirmPassword -> {
                    confirmPasswordLayout.error = "Passwords do not match"
                }

                !PasswordValidator.isValid(password) -> {
                    passwordLayout.error = "Min 8 chars, at least 1 letter & 1 number"
                }

                else -> {
                    passwordLayout.error = null
                    confirmPasswordLayout.error = null
                    registerUser(name, surname, email, password)
                }
            }
        }
    }

    private fun registerUser(name: String, surname: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result!!.user!!
                    val user = User(firebaseUser.uid, name, surname, email)
                    FirestoreClass().registerUser(
                        user,
                        onSuccess = {
                            val prefs = getSharedPreferences("lunaflow_prefs", MODE_PRIVATE)
                            prefs.edit { putBoolean("remember_me", true) }

                            startActivity(Intent(this, MainActivity::class.java))
                            finishAffinity()
                        },
                        onFailure = {
                            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}
