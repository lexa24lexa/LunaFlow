package com.example.lunaflow.utils

object PasswordValidator {

    private val PASSWORD_REGEX =
        Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")

    fun isValid(password: String): Boolean {
        return PASSWORD_REGEX.matches(password)
    }
}
