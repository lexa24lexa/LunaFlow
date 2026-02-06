package com.example.lunaflow.models

sealed class HistoryItem {
    data class DateHeader(val date: String) : HistoryItem()
    data class Log(val logEntry: LogEntry) : HistoryItem()
}
