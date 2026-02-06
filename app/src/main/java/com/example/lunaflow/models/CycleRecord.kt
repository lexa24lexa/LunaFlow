package com.example.lunaflow.models

data class CycleRecord(
    val id: String = "",
    val date: String = "",
    val phase: String = "",
    val flow: String? = null,
    val logs: List<LogEntry> = emptyList(),
    val isManual: Boolean = true
)
