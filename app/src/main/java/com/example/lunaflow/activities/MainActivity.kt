package com.example.lunaflow.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.CycleAdapter
import com.example.lunaflow.models.CycleRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CycleAdapter
    private val cycleList = mutableListOf<CycleRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CycleAdapter(cycleList)
        recyclerView.adapter = adapter

        loadCycles()
    }

    private fun loadCycles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("cycles")
            .get()
            .addOnSuccessListener { querySnapshot ->
                cycleList.clear()

                querySnapshot.documents.forEach { doc ->
                    val cycle = doc.toObject(CycleRecord::class.java)
                    if (cycle != null) {
                        cycleList.add(cycle)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
