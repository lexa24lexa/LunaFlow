package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R

class NotificationsAdapter(private val items: List<String>) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    // view holder da notificação
    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.textNotification)
    }

    // cria o view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    // associa mensagem à view
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.message.text = items[position]
    }

    // número total de notificações
    override fun getItemCount(): Int = items.size
}
