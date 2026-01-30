package com.example.lunaflow.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.HistoryItem

class HistoryAdapter(private var items: List<HistoryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // tipo header de data
        private const val TYPE_HEADER = 0

        // tipo log individual
        private const val TYPE_LOG = 1
    }

    // atualiza lista do histórico
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // define tipo de item
    override fun getItemViewType(position: Int): Int {
        return when(items[position]) {
            is HistoryItem.DateHeader -> TYPE_HEADER
            is HistoryItem.LogEntry -> TYPE_LOG
        }
    }

    // cria view holder conforme o tipo
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

    // faz bind dos dados
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is DateHeaderViewHolder -> holder.bind(items[position] as HistoryItem.DateHeader)
            is LogEntryViewHolder -> holder.bind(items[position] as HistoryItem.LogEntry)
        }
    }

    // número total de itens
    override fun getItemCount() = items.size

    // view holder do header de data
    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.dateText)

        // mostra a data
        fun bind(item: HistoryItem.DateHeader) {
            dateText.text = item.date
        }
    }

    // view holder do log
    class LogEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timeText: TextView = view.findViewById(R.id.entryTime)
        private val titleText: TextView = view.findViewById(R.id.entryTitle)
        private val detailsText: TextView = view.findViewById(R.id.entryDetails)
        private val icon: ImageView = view.findViewById(R.id.entryIcon)

        // mostra dados do log
        fun bind(item: HistoryItem.LogEntry) {
            timeText.text = item.time
            titleText.text = item.title
            detailsText.text = item.details

            // define ícone conforme tipo
            icon.setImageResource(
                when(item.type) {
                    "activity" -> R.drawable.ic_activity
                    "pill" -> R.drawable.ic_pill
                    "symptoms" -> R.drawable.ic_symptom
                    else -> R.drawable.ic_event
                }
            )
        }
    }
}
