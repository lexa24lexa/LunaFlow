package com.example.lunaflow.models

data class CycleRecord(
    val id: String = "",
    val date: String = "",
    val phase: String = "",
    val symptoms: Map<String, Boolean> = emptyMap(),
    val logs: Map<String, Any> = emptyMap()
)