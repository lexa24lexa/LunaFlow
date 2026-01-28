package com.example.lunaflow.data

// A sealed class to represent different types of items in your RecyclerView
sealed class HistoryItem {
    data class DateHeader(val date: String) : HistoryItem()
    data class LogEntry(
        val time: String,
        val type: String,
        val title: String,
        val details: String
    ) : HistoryItem()
}

// types for filter
enum class HistoryFilterTag {
    SEX,
    MASTURBATION,
    PLAN_B,
    BIRTH_CONTROL,
    OTHER_MEDICATION,
    PHYSICAL_SYMPTOMS,
    EMOTIONAL_SYMPTOMS,
    FLOW
}
