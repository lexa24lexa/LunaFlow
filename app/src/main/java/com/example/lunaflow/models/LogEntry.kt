package com.example.lunaflow.models

data class LogEntry(
    val id: String = "",
    val cycleId: String = "",
    val type: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val details: String = "",
    val data: Map<String, Any> = emptyMap()
)
