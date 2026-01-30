package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.CycleRecord

class CycleAdapter(
    private val records: List<CycleRecord>
) : RecyclerView.Adapter<CycleAdapter.CycleViewHolder>() {

    // view holder do ciclo
    inner class CycleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSymptoms: TextView = view.findViewById(R.id.tvSymptoms)
    }

    // cria view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cycle, parent, false)
        return CycleViewHolder(view)
    }

    // faz bind dos dados do ciclo
    override fun onBindViewHolder(holder: CycleViewHolder, position: Int) {
        val record = records[position]
        holder.tvDate.text = record.date

        // mostra sintomas ativos ou mensagem default
        holder.tvSymptoms.text = if (record.symptoms.isNotEmpty()) {
            record.symptoms.filter { it.value }.keys.joinToString(", ")
        } else {
            "No symptoms logged"
        }
    }

    // número total de itens
    override fun getItemCount(): Int = records.size
}
