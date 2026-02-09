package com.example.lunaflow.models

data class UserCycleProfile(
    var lastPeriodStart: String = "",
    var cycleLength: Int = 28,
    var periodLength: Int = 5
)