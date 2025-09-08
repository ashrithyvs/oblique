package com.example.oblique_android.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import android.view.View
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

class GoalTypeAdapter(
    private val onSelect: (GoalType) -> Unit
) : RecyclerView.Adapter<GoalTypeAdapter.VH>() {

    data class GoalType(val title: String, val suggested: Int, val unit: String)

    private val items = listOf(
        GoalType("Solve coding problems", 2, "problems"),
        GoalType("Study time", 30, "minutes"),
        GoalType("Read pages", 10, "pages"),
        GoalType("Meditation", 10, "minutes")
    )

    private var selectedIndex = -1

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_type, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = "Suggested: ${item.suggested} ${item.unit}"
        holder.card.isChecked = position == selectedIndex

        // Capture the data object and pass it (not the view)
        holder.card.setOnClickListener {
            val old = selectedIndex
            selectedIndex = position
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
            onSelect(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun select(gt: GoalType) {
        val idx = items.indexOfFirst { it.title == gt.title && it.unit == gt.unit }
        if (idx >= 0) {
            val old = selectedIndex
            selectedIndex = idx
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
        }
    }

    fun clearSelection() {
        val old = selectedIndex
        selectedIndex = -1
        if (old >= 0) notifyItemChanged(old)
    }
}
