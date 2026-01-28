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
import com.example.lunaflow.data.HistoryItem
import com.google.firebase.firestore.DocumentSnapshot
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_history)

        // toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.historyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "History"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(filteredItems)
        recyclerView.adapter = adapter

        fetchHistory()

        setToolbarTitle("LunaFlow")
        setupBottomNav(R.id.nav_history)
        showToolbar(true)
        showBottomNav(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                allItems.clear()
                physicalSymptoms.clear()
                emotionalSymptoms.clear()

                val sdfDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                var lastDate = ""

                for (doc in result) {
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue
                    val dateStr = sdfDate.format(timestamp)

                    if (dateStr != lastDate) {
                        allItems.add(HistoryItem.DateHeader(dateStr))
                        lastDate = dateStr
                    }

                    val type = doc.getString("type") ?: "unknown"
                    val activityMap = doc.getMap("activity")
                    val symptomsMap = doc.getMap("symptoms")

                    // dynamic symptoms
                    symptomsMap.forEach { (key, value) ->
                        if (value == true) {
                            when(key.lowercase(Locale.getDefault())) {
                                "mood swings","anxiety","irritability" -> emotionalSymptoms.add(key)
                                else -> if (key != "otherSymptoms") physicalSymptoms.add(key)
                            }
                        }
                    }

                    val title = when (type) {
                        "activity" -> {
                            val selected = activityMap.filterValues { it == true }.keys
                            if (selected.isNotEmpty()) selected.joinToString(", ") else "Activity"
                        }
                        "symptoms" -> {
                            val flow = doc.getString("flow") ?: "None"
                            if (flow != "None") "Flow: $flow"
                            else {
                                val selected = symptomsMap.filterValues { it == true }.keys
                                if (selected.isNotEmpty()) selected.joinToString(", ") else "Symptoms"
                            }
                        }
                        else -> "Log"
                    }

                    val details = when (type) {
                        "activity" -> {
                            val list = mutableListOf<String>()
                            if (activityMap["sex"] == true) {
                                list.add("Sex")
                                list.add("Protection: ${activityMap["protectionUsed"] ?: ""}")
                            }
                            if (activityMap["planBPill"] == true) {
                                list.add("Plan B: ${activityMap["pillType"] ?: ""}")
                                list.add("Time since: ${activityMap["timeSinceIntercourse"] ?: ""}")
                            }
                            val med = activityMap["otherMedication"] as? String
                            if (!med.isNullOrBlank()) list.add("Medication: $med")
                            list.joinToString("\n")
                        }
                        "symptoms" -> {
                            val list = symptomsMap.filterValues { it == true }.keys.toMutableList()
                            val other = symptomsMap["otherSymptoms"] as? String
                            if (!other.isNullOrBlank()) list.add(other)
                            list.joinToString(", ")
                        }
                        else -> ""
                    }

                    allItems.add(
                        HistoryItem.LogEntry(
                            time = sdfTime.format(timestamp),
                            type = type,
                            title = title,
                            details = details
                        )
                    )
                }

                filteredItems.clear()
                filteredItems.addAll(allItems)
                adapter.updateList(filteredItems)
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

        // pre-check saved selections
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
            if (item is HistoryItem.LogEntry) {
                val matched = selectedFilters.any { filter ->
                    when(filter) {
                        "Birth control" -> item.details.contains("Birth control", ignoreCase = true) ||
                                item.title.contains("Birth control", ignoreCase = true)
                        "Other medication" -> item.details.contains("Medication", ignoreCase = true) ||
                                item.title.contains("Other medication", ignoreCase = true)
                        "Flow" -> item.title.contains("Flow", ignoreCase = true)
                        else -> item.title.contains(filter, ignoreCase = true) ||
                                item.details.contains(filter, ignoreCase = true)
                    }
                }
                if (matched) filteredItems.add(item)
            } else {
                filteredItems.add(item)
            }
        }

        adapter.updateList(filteredItems)
    }

    private fun DocumentSnapshot.getMap(key: String): Map<String, Any?> {
        val raw = get(key)
        return if (raw is Map<*, *>) {
            raw.filterKeys { it is String }.mapKeys { it.key as String }
        } else {
            emptyMap()
        }
    }
}
