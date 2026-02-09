package com.example.lunaflow.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.*

class DailySymptomResetWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("daily_symptoms", Context.MODE_PRIVATE)
        prefs.edit {
            putString("last_date", SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()))
        }
        return Result.success()
    }
}
