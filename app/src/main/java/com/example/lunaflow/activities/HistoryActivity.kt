package com.example.lunaflow.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.HistoryAdapter
import com.example.lunaflow.models.CycleRecord
import com.example.lunaflow.models.HistoryItem
import com.example.lunaflow.models.LogEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val allItems = mutableListOf<HistoryItem>()
    private val filteredItems = mutableListOf<HistoryItem>()
    private val selectedFiltersMemory = mutableSetOf<String>()
    private val physicalSymptoms = mutableSetOf<String>()
    private val emotionalSymptoms = mutableSetOf<String>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "History"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(allItems, onLogClick = { showLogOptions(it) })
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

        db.collection("users")
            .document(userId)
            .collection("cycle_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                allItems.clear()
                physicalSymptoms.clear()
                emotionalSymptoms.clear()

                val sdfDisplay = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val sdfRecord = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var lastDate = ""

                for (doc in snapshot.documents) {
                    val record = doc.toObject(CycleRecord::class.java) ?: continue
                    val recordDate = sdfRecord.parse(record.date) ?: continue
                    val dateStr = sdfDisplay.format(recordDate)

                    if (dateStr != lastDate) {
                        allItems.add(HistoryItem.DateHeader(dateStr))
                        lastDate = dateStr
                    }

                    record.logs.sortedByDescending { it.timestamp }.forEach { log ->
                        // collect symptoms for filters
                        if (log.type == "symptoms") {
                            log.data.keys.forEach { key ->
                                val keyLower = key.lowercase(Locale.getDefault())
                                if (keyLower in listOf("mood swings", "anxiety", "irritability")) emotionalSymptoms.add(key)
                                else if (keyLower != "othersymptoms") physicalSymptoms.add(key)
                            }
                        }
                        allItems.add(HistoryItem.Log(log))
                    }
                }

                filteredItems.clear()
                filteredItems.addAll(allItems)
                adapter.updateList(filteredItems)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun showLogOptions(logItem: HistoryItem.Log) {
        val options = arrayOf("View Details", "Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showLogDetails(logItem)
                    1 -> editLog(logItem)
                    2 -> confirmDeleteLog(logItem)
                }
            }
            .show()
    }

    private fun showLogDetails(logItem: HistoryItem.Log) {
        val log = logItem.logEntry
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val message = buildString {
            append("Time: ${sdfTime.format(Date(log.timestamp))}\n")
            append("Type: ${when (log.type) {
                "activity" -> "Activity"
                "symptoms" -> "Symptom"
                else -> "Log"
            }}\n\n")
            if (log.title.isNotEmpty()) append("Title: ${log.title}\n")
            if (log.details.isNotEmpty()) append("Details: ${log.details}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Log Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun editLog(logItem: HistoryItem.Log) {
        val log = logItem.logEntry
        // TODO: open ActivityLogActivity or SymptomLogActivity with log.cycleId + log.id
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> { showFilterDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_history_filters, null)
        val cbSex = dialogView.findViewById<CheckBox>(R.id.filterSex)
        val cbMasturbation = dialogView.findViewById<CheckBox>(R.id.filterMasturbation)
        val cbPlanB = dialogView.findViewById<CheckBox>(R.id.filterPlanB)
        val cbBirthControl = dialogView.findViewById<CheckBox>(R.id.filterBirthControl)
        val cbOtherMed = dialogView.findViewById<CheckBox>(R.id.filterOtherMed)
        val cbPhysical = dialogView.findViewById<CheckBox>(R.id.filterPhysical)
        val cbEmotional = dialogView.findViewById<CheckBox>(R.id.filterEmotional)
        val cbFlow = dialogView.findViewById<CheckBox>(R.id.filterFlow)

        cbSex.isChecked = selectedFiltersMemory.contains("Sex")
        cbMasturbation.isChecked = selectedFiltersMemory.contains("Masturbation")
        cbPlanB.isChecked = selectedFiltersMemory.contains("Plan B")
        cbBirthControl.isChecked = selectedFiltersMemory.contains("Birth control")
        cbOtherMed.isChecked = selectedFiltersMemory.contains("Other medication")
        cbPhysical.isChecked = selectedFiltersMemory.intersect(physicalSymptoms).isNotEmpty()
        cbEmotional.isChecked = selectedFiltersMemory.intersect(emotionalSymptoms).isNotEmpty()
        cbFlow.isChecked = selectedFiltersMemory.contains("Flow")

        AlertDialog.Builder(this)
            .setTitle("Filter History")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                selectedFiltersMemory.clear()
                if (cbSex.isChecked) selectedFiltersMemory.add("Sex")
                if (cbMasturbation.isChecked) selectedFiltersMemory.add("Masturbation")
                if (cbPlanB.isChecked) selectedFiltersMemory.add("Plan B")
                if (cbBirthControl.isChecked) selectedFiltersMemory.add("Birth control")
                if (cbOtherMed.isChecked) selectedFiltersMemory.add("Other medication")
                if (cbPhysical.isChecked) selectedFiltersMemory.addAll(physicalSymptoms)
                if (cbEmotional.isChecked) selectedFiltersMemory.addAll(emotionalSymptoms)
                if (cbFlow.isChecked) selectedFiltersMemory.add("Flow")
                applyFilters(selectedFiltersMemory.toList())
            }
            .setNeutralButton("Clear Filters") { _, _ ->
                selectedFiltersMemory.clear()
                filteredItems.clear()
                filteredItems.addAll(allItems)
                adapter.updateList(filteredItems)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyFilters(selectedFilters: List<String>) {
        filteredItems.clear()
        allItems.forEach { item ->
            if (item is HistoryItem.Log) {
                val log = item.logEntry
                val matched = selectedFilters.any { filter ->
                    when(filter) {
                        "Birth control" -> log.details.contains("Birth control", ignoreCase = true) ||
                                log.title.contains("Birth control", ignoreCase = true)
                        "Other medication" -> log.details.contains("Medication", ignoreCase = true) ||
                                log.title.contains("Other medication", ignoreCase = true)
                        "Flow" -> log.title.contains("Flow", ignoreCase = true)
                        else -> log.title.contains(filter, ignoreCase = true) ||
                                log.details.contains(filter, ignoreCase = true)
                    }
                }
                if (matched) filteredItems.add(item)
            } else {
                filteredItems.add(item)
            }
        }
        adapter.updateList(filteredItems)
    }
}
