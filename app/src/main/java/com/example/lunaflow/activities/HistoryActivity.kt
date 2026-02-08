package com.example.lunaflow.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.HistoryAdapter
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.HistoryItem
import com.example.lunaflow.models.LogEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val allItems = mutableListOf<HistoryItem>()
    private val filteredItems = mutableListOf<HistoryItem>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        setSupportActionBar(toolbar)
        setToolbarTitle("LunaFlow")
        supportActionBar?.title = "History"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(
            allItems,
            onLogClick = { showLogDetails(it) },
            onDeleteClick = { confirmDeleteLog(it) }
        )

        recyclerView.adapter = adapter

        fetchHistory()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("cycle_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                allItems.clear()

                val sdfDisplay = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val sdfRecord = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var lastDate = ""

                for (doc in snapshot.documents) {
                    val record = doc.toObject(CycleRecord::class.java) ?: continue
                    val cycleId = doc.id
                    val recordDate = sdfRecord.parse(record.date) ?: continue
                    val dateStr = sdfDisplay.format(recordDate)

                    if (dateStr != lastDate) {
                        allItems.add(HistoryItem.DateHeader(dateStr))
                        lastDate = dateStr
                    }

                    record.logs.sortedByDescending { it.timestamp }.forEach { log ->
                        allItems.add(
                            HistoryItem.Log(
                                log.copy(cycleId = cycleId)
                            )
                        )
                    }
                }

                filteredItems.clear()
                filteredItems.addAll(allItems)
                adapter.updateList(filteredItems)
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    private fun showLogDetails(logItem: HistoryItem.Log) {
        val log = logItem.logEntry
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

        val message = buildString {
            append("Time: ${sdfTime.format(Date(log.timestamp))}\n")
            append("Type: ${when (log.type) {
                "activity" -> "Activity"
                "symptoms" -> "Symptoms"
                else -> "Log"
            }}\n\n")

            if (log.title.isNotBlank() && log.title.lowercase() !in listOf("activity", "symptoms")) {
                append("Title: ${log.title}\n")
            }

            if (log.details.isNotBlank()) {
                append("Details: ${log.details}\n")
            }

            if (log.data.isNotEmpty()) {
                val selectedData = log.data.filter { (_, value) ->
                    when (value) {
                        is Boolean -> value
                        is String -> value.isNotBlank()
                        is Number -> true
                        else -> value != null
                    }
                }

                if (selectedData.isNotEmpty()) {
                    append("\nDetails:\n")
                    selectedData.forEach { (key, value) ->
                        if (value is Boolean) {
                            append("• $key\n")
                        } else {
                            append("• $key: $value\n")
                        }
                    }
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Log Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmDeleteLog(logItem: HistoryItem.Log) {
        AlertDialog.Builder(this)
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this log?")
            .setPositiveButton("Delete") { _, _ -> deleteLog(logItem) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLog(logItem: HistoryItem.Log) {
        val log = logItem.logEntry
        val userId = auth.currentUser?.uid ?: return

        val cycleRef = db.collection("users")
            .document(userId)
            .collection("cycle_records")
            .document(log.cycleId)

        cycleRef.get().addOnSuccessListener { doc ->
            val record = doc.toObject(CycleRecord::class.java) ?: return@addOnSuccessListener
            val updatedLogs = record.logs.filter { it.id != log.id }

            cycleRef.update("logs", updatedLogs)
                .addOnSuccessListener { fetchHistory() }
                .addOnFailureListener { it.printStackTrace() }
        }
    }
}
