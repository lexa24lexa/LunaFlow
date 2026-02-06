package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.HistoryItem
import com.example.lunaflow.models.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onLogClick: (HistoryItem.Log) -> Unit,
    private val onDeleteClick: (HistoryItem.Log) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LOG = 1
    }

    fun updateList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is HistoryItem.DateHeader) TYPE_HEADER else TYPE_LOG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_date, parent, false)
            DateHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_entry, parent, false)
            LogEntryViewHolder(view)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is DateHeaderViewHolder && item is HistoryItem.DateHeader) {
            holder.bind(item)
        }
        if (holder is LogEntryViewHolder && item is HistoryItem.Log) {
            holder.bind(item.logEntry, item)
        }
    }

    override fun getItemCount() = items.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.dateText)
        fun bind(item: HistoryItem.DateHeader) {
            dateText.text = item.date
        }
    }

    inner class LogEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timeText: TextView = view.findViewById(R.id.entryTime)
        private val typeText: TextView = view.findViewById(R.id.entryType)
        private val icon: ImageView = view.findViewById(R.id.entryIcon)
        private val editButton: ImageButton = view.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

        fun bind(log: LogEntry, wrapper: HistoryItem.Log) {

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = sdf.format(Date(log.timestamp))

            typeText.text = when (log.type) {
                "activity" -> "Activity"
                "symptoms" -> "Symptom"
                else -> "Log"
            }

            icon.setImageResource(
                when (log.type) {
                    "activity" -> R.drawable.ic_activity
                    "symptoms" -> R.drawable.ic_symptom
                    else -> R.drawable.ic_event
                }
            )

            itemView.setOnClickListener { onLogClick(wrapper) }

            deleteButton.setOnClickListener {
                onDeleteClick(wrapper)
            }

            editButton.setOnClickListener {
                // reservado para Update depois
            }
        }
    }
}
