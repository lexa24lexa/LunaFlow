package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.LogEntry
import com.example.lunaflow.models.LogType
import com.example.lunaflow.models.UserCycleProfile
import com.example.lunaflow.utils.PredictionEngine
import java.text.SimpleDateFormat
import java.util.*

class CycleAdapter(
    private val logsByDate: Map<String, List<LogEntry>>,
    private val userProfile: UserCycleProfile
) : RecyclerView.Adapter<CycleAdapter.CycleViewHolder>() {

    inner class CycleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSymptoms: TextView = view.findViewById(R.id.tvSymptoms)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cycle, parent, false)
        return CycleViewHolder(view)
    }

    override fun onBindViewHolder(holder: CycleViewHolder, position: Int) {
        val dateStr = logsByDate.keys.elementAt(position)
        val logs = logsByDate[dateStr] ?: emptyList()

        holder.tvDate.text = dateStr

        val symptoms = logs.filter { it.type == LogType.symptoms }
            .flatMap { it.data.keys }
            .distinct()

        holder.tvSymptoms.text = if (symptoms.isNotEmpty()) symptoms.joinToString(", ") else "No symptoms logged"
    }

    override fun getItemCount(): Int = logsByDate.size
}
