package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R

class SymptomAdviceAdapter(private val symptomList: List<String>) :
    RecyclerView.Adapter<SymptomAdviceAdapter.SymptomViewHolder>() {

    inner class SymptomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val symptomText: TextView = itemView.findViewById(R.id.symptomText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_symptom_advice, parent, false)
        return SymptomViewHolder(view)
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        holder.symptomText.text = symptomList[position]
    }

    override fun getItemCount() = symptomList.size
}
