package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.Advice

class AdviceAdapter(private val adviceList: List<Advice>) :
    RecyclerView.Adapter<AdviceAdapter.AdviceViewHolder>() {

    inner class AdviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adviceText: TextView = itemView.findViewById(R.id.adviceItemText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_advice, parent, false)
        return AdviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdviceViewHolder, position: Int) {
        val advice = adviceList[position]
        holder.adviceText.text = advice.adviceText
    }

    override fun getItemCount(): Int = adviceList.size
}
