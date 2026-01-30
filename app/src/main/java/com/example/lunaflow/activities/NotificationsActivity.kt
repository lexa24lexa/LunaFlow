package com.example.lunaflow.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_notifications)
        setToolbarTitle("Notificações")

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationsAdapter(notificationsList)
        recyclerView.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications")
            .get()
            .addOnSuccessListener { result ->
                notificationsList.clear()
                for (document in result) {
                    notificationsList.add(document.getString("message") ?: "")
                }
                adapter.notifyDataSetChanged()
            }
    }
}
