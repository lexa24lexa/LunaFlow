package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.data.HistoryItem

class HistoryAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LOG = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when(items[position]) {
            is HistoryItem.DateHeader -> TYPE_HEADER
            is HistoryItem.LogEntry -> TYPE_LOG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_date, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_LOG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_entry, parent, false)
                LogEntryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is DateHeaderViewHolder -> holder.bind(items[position] as HistoryItem.DateHeader)
            is LogEntryViewHolder -> holder.bind(items[position] as HistoryItem.LogEntry)
        }
    }

    override fun getItemCount() = items.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.dateText)
        fun bind(item: HistoryItem.DateHeader) {
            dateText.text = item.date
        }
    }

    class LogEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timeText: TextView = view.findViewById(R.id.entryTime)
        private val titleText: TextView = view.findViewById(R.id.entryTitle)
        private val detailsText: TextView = view.findViewById(R.id.entryDetails)
        private val icon: ImageView = view.findViewById(R.id.entryIcon)

        fun bind(item: HistoryItem.LogEntry) {
            timeText.text = item.time
            titleText.text = item.title
            detailsText.text = item.details
            icon.setImageResource(
                when(item.type) {
                    "activity" -> R.drawable.ic_activity
                    "pill" -> R.drawable.ic_pill
                    "symptom" -> R.drawable.ic_symptom
                    else -> R.drawable.ic_event
                }
            )
        }
    }
}
