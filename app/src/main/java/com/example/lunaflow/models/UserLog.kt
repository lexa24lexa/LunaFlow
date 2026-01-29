package com.example.lunaflow.models

import java.util.*

data class UserLog(
    val userId: String = "",
    val type: String = "",
    val phase: String = "",
    val flow: String = "",
    val symptoms: Map<String, Boolean> = emptyMap(),
    val otherSymptoms: String = "",
    val timestamp: Date = Date()
)
