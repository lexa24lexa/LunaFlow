package com.example.lunaflow.models

data class Advice(
    val phase: String = "",
    val adviceText: String = ""
)

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