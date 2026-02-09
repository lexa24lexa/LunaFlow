package com.example.lunaflow.utils

import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.UserCycleProfile
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object PredictionEngine {

    private const val OVULATION_OFFSET = 14

    /**
     * Calculate cycle phase for a given date using logs + fallback to profile
     */
    fun calculatePhase(date: Date, profile: UserCycleProfile, logs: List<LogEntry>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateStr = sdf.format(date)

        // find all flow logs: type="flow" OR type="symptoms" with a "flow" field
        val flowLogs = logs.filter { log ->
            (log.type.equals("flow", ignoreCase = true) && log.data["flow"] is String) ||
                    (log.type.equals("symptoms", ignoreCase = true) && log.data["flow"] is String)
        }.sortedByDescending { it.date }

        // if a flow log exists for this day, it's menstruation
        flowLogs.find { it.date == dateStr }?.let { return "Menstruation" }

        // determine last period start date
        val lastPeriodStart = flowLogs.firstOrNull()?.date ?: profile.lastPeriodStart
        if (lastPeriodStart.isEmpty()) return "Unknown"

        val start = sdf.parse(lastPeriodStart)!!
        val diffDays = ((date.time - start.time) / (1000 * 60 * 60 * 24)).toInt()

        // predict average cycle length from logs or use profile
        val lastCycles = flowLogs.take(6)
        val cycleLengths = mutableListOf<Int>()
        for (i in 0 until lastCycles.size - 1) {
            val thisDate = sdf.parse(lastCycles[i].date)!!
            val nextDate = sdf.parse(lastCycles[i + 1].date)!!
            val length = ((thisDate.time - nextDate.time) / (1000 * 60 * 60 * 24)).toInt().let { if (it > 0) it else -it }
            cycleLengths.add(length)
        }
        val averageCycleLength = if (cycleLengths.isNotEmpty()) cycleLengths.average().roundToInt() else profile.cycleLength

        val ovulationDay = averageCycleLength - OVULATION_OFFSET

        // determine current phase
        val dayInCycle = diffDays % averageCycleLength
        return when {
            dayInCycle in 0 until profile.periodLength -> "Menstruation"
            dayInCycle in profile.periodLength until ovulationDay -> "Follicular"
            dayInCycle == ovulationDay -> "Ovulation"
            dayInCycle in (ovulationDay + 1) until averageCycleLength -> "Luteal"
            else -> "Unknown"
        }
    }
}
