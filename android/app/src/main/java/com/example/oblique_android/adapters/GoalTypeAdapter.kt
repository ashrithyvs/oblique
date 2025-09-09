package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.models.GoalType

// Lightweight data holder for goal types


class GoalTypeAdapter(
    private val items: List<GoalType>,
    private val onClick: (GoalType) -> Unit
) : RecyclerView.Adapter<GoalTypeAdapter.VH>() {

    private var selectedIndex = -1

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvGoalTypeTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvGoalTypeSubtitle)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    selectedIndex = pos
                    notifyDataSetChanged()
                    onClick(items[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_type, parent, false) // IMPORTANT: parent, false
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvTitle.text = it.title
        holder.tvSubtitle.text = it.subtitle

        // selection visual
        holder.itemView.isSelected = (position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    fun select(goalType: GoalType) {
        val pos = items.indexOfFirst { it.id == goalType.id }
        if (pos >= 0) {
            selectedIndex = pos
            notifyDataSetChanged()
        }
    }

    fun clearSelection() {
        selectedIndex = -1
        notifyDataSetChanged()
    }
}
