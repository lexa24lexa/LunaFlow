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
