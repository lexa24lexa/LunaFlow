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
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val allItems = mutableListOf<HistoryItem>()
    private val filteredItems = mutableListOf<HistoryItem>()
    private val db = FirebaseFirestore.getInstance()

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        setSupportActionBar(toolbar)
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

        startListeningToHistory()

        setToolbarTitle("LunaFlow")
        setupBottomNav(R.id.nav_history)
        showToolbar(true)
        showBottomNav(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun startListeningToHistory() {
        val userId = auth.currentUser?.uid ?: return

        listenerRegistration = db.collection("users").document(userId)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { error.printStackTrace(); return@addSnapshotListener }
                snapshot?.let { buildHistoryList(it.documents) }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun buildHistoryList(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        allItems.clear()
        val sdfDisplay = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        var lastDate = ""

        val logs = documents.mapNotNull { doc ->
            try { doc.toObject(LogEntry::class.java)?.copy(id = doc.id) } catch(e: Exception) { null }
        }

        logs.sortedByDescending { it.timestamp }.forEach { log ->
            if (log.date != lastDate) {
                allItems.add(HistoryItem.DateHeader(sdfDisplay.format(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(log.date)!!)))
                lastDate = log.date
            }
            allItems.add(HistoryItem.Log(log))
        }

        filteredItems.clear()
        filteredItems.addAll(allItems)
        adapter.updateList(filteredItems)
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
                        if (value is Boolean) append("• $key\n")
                        else append("• $key: $value\n")
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

        val logsRef = db.collection("users").document(userId).collection("logs")
        logsRef.document(log.id).delete()
            .addOnSuccessListener { println("Log deleted successfully") }
            .addOnFailureListener { it.printStackTrace() }
    }
}
