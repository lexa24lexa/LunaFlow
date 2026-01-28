package com.example.lunaflow.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.HistoryAdapter
import com.example.lunaflow.data.HistoryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val historyItems = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyItems)
        recyclerView.adapter = adapter

        fetchHistory()

        setToolbarTitle("LunaFlow")
        setupBottomNav(R.id.nav_history)
        showToolbar(true)
        showBottomNav(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchHistory() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val groupedItems = mutableListOf<HistoryItem>()

                val sdfDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

                var lastDate = ""
                for (doc in result) {
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue
                    val dateStr = sdfDate.format(timestamp)

                    // date header
                    if (dateStr != lastDate) {
                        groupedItems.add(HistoryItem.DateHeader(dateStr))
                        lastDate = dateStr
                    }

                    // type
                    val activityMap = doc.get("activity") as? Map<String, Any> ?: emptyMap()
                    val symptomsMap = doc.get("symptoms") as? Map<String, Any> ?: emptyMap()
                    val type = when {
                        activityMap.isNotEmpty() -> "activity"
                        symptomsMap.isNotEmpty() -> "symptom"
                        else -> "pill"
                    }

                    // title and details
                    val selectedActivity = activityMap?.filter { it.value == true }?.keys ?: emptySet()
                    val title = when (type) {
                        "activity" -> if (selectedActivity.isNotEmpty()) selectedActivity.joinToString(", ") else "Activity"
                        "symptom" -> {
                            val selectedSymptoms = symptomsMap?.filter { it.value == true }?.keys ?: emptySet()
                            if (selectedSymptoms.isNotEmpty()) selectedSymptoms.joinToString(", ") else "Symptoms"
                        }
                        else -> doc.getString("pillName") ?: "Pill"
                    }

                    val details = when (type) {
                        "activity" -> {
                            val detailsList = mutableListOf<String>()
                            activityMap?.forEach { (key, value) ->
                                if (value == true) {
                                    when (key) {
                                        "sex" -> {
                                            val protection = (activityMap["protectionUsed"] ?: "") as String
                                            val sexDetails = (activityMap["sexDetails"] ?: "") as String
                                            detailsList.add("Protection used: $protection")
                                            if (sexDetails.isNotEmpty()) detailsList.add(sexDetails)
                                        }
                                        "planBPill" -> {
                                            val pillType = (activityMap["pillType"] ?: "") as String
                                            val timeSince = (activityMap["timeSinceIntercourse"] ?: "") as String
                                            val planBDetails = (activityMap["planBDetails"] ?: "") as String
                                            detailsList.add("Pill: $pillType")
                                            detailsList.add("Time since intercourse: $timeSince")
                                            if (planBDetails.isNotEmpty()) detailsList.add(planBDetails)
                                        }
                                        "otherMedication" -> {
                                            val med = (activityMap["otherMedicationName"] ?: "") as String
                                            if (med.isNotEmpty()) detailsList.add("Medication: $med")
                                        }
                                    }
                                }
                            }
                            detailsList.joinToString("\n")
                        }
                        "symptom" -> {
                            symptomsMap?.filter { it.value == true }?.keys?.joinToString(", ") ?: ""
                        }
                        else -> doc.getString("pillDetails") ?: ""
                    }

                    groupedItems.add(
                        HistoryItem.LogEntry(
                            time = sdfTime.format(timestamp),
                            type = type,
                            title = title,
                            details = details
                        )
                    )
                }

                adapter = HistoryAdapter(groupedItems)
                recyclerView.adapter = adapter
            }
    }
}
