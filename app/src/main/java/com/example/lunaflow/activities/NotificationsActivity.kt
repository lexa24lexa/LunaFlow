package com.example.lunaflow.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.adapters.NotificationsAdapter
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = mutableListOf<String>()

    // inicializa activity e recycler view
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_notifications)
        setToolbarTitle("LunaFlow")
        setupBottomNav()
        showToolbar(true)
        showBottomNav(true)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationsAdapter(notificationsList)
        recyclerView.adapter = adapter

        fetchNotifications()
    }

    // busca notificações do firestore
    private fun fetchNotifications() {

        val currentUser = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser ?: return

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(currentUser.uid)
            .collection("notifications")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->

                notificationsList.clear()

                for (document in result) {
                    val message = document.getString("message") ?: ""
                    notificationsList.add(message)
                }

                adapter.notifyDataSetChanged()
            }
    }
}
