package com.example.lunaflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lunaflow.R
import com.example.lunaflow.models.Advice

class AdviceAdapter(
    private val adviceList: List<Advice>
) : RecyclerView.Adapter<AdviceAdapter.AdviceViewHolder>() {

    // view holder do advice
    inner class AdviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adviceText: TextView = itemView.findViewById(R.id.adviceItemText)
    }

    // cria view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_advice, parent, false)
        return AdviceViewHolder(view)
    }

    // faz bind do advice ao item
    override fun onBindViewHolder(holder: AdviceViewHolder, position: Int) {
        val advice = adviceList[position]
        holder.adviceText.text = advice.adviceText
    }

    // número total de itens
    override fun getItemCount(): Int {
        return adviceList.size
    }
}
