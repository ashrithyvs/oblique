package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.models.GoalType
import com.example.oblique_android.utils.TileSelectionStyle
import com.google.android.material.card.MaterialCardView

class GoalTypeAdapter(
    private val items: List<GoalType>,
    private val onClick: (GoalType) -> Unit
) : RecyclerView.Adapter<GoalTypeAdapter.VH>() {

    private var selectedIndex = -1

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardGoalType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvGoalTypeTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvGoalTypeSubtitle)

        init {
            card.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val previous = selectedIndex
                selectedIndex = pos
                if (previous >= 0) notifyItemChanged(previous)
                notifyItemChanged(selectedIndex)
                onClick(items[pos])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_type, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvTitle.text = it.title
        holder.tvSubtitle.text = it.subtitle
        TileSelectionStyle.apply(holder.card, position == selectedIndex, holder.itemView.context)
    }

    override fun getItemCount(): Int = items.size

    fun select(goalType: GoalType) {
        val pos = items.indexOfFirst { it.id == goalType.id }
        if (pos >= 0 && pos != selectedIndex) {
            val previous = selectedIndex
            selectedIndex = pos
            if (previous >= 0) notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
        }
    }

    fun clearSelection() {
        val previous = selectedIndex
        selectedIndex = -1
        if (previous >= 0) notifyItemChanged(previous)
    }
}
