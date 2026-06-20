package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.models.Goal
import com.google.android.material.card.MaterialCardView

class GoalsAdapter(
    private var goals: List<Goal> = emptyList(),
    private val onDelete: (Goal) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val rootCard: MaterialCardView? = view.findViewById(R.id.rootCard)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvPlatformName: TextView = view.findViewById(R.id.tvPlatformName)
        val tvGoalSummary: TextView = view.findViewById(R.id.tvGoalSummary)
        val tvProgress: TextView = view.findViewById(R.id.tvProgress)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = goals[position]
        holder.tvPlatformName.text = g.platform
        val unitText = when (g.unit) {
            "minutes" -> "minutes"
            "pages" -> "pages"
            else -> g.unit
        }
        holder.tvGoalSummary.text = "${g.targetValue} $unitText daily"
        holder.tvProgress.text = if (g.progress >= g.targetValue) "Done" else "${g.progress}/${g.targetValue}"

        val iconRes = when (g.platform) {
            "LeetCode" -> R.drawable.ic_leetcode
            "Duolingo" -> R.drawable.ic_duolingo
            else -> R.drawable.ic_launcher_foreground
        }
        holder.ivIcon.setImageResource(iconRes)

        holder.btnDelete.setOnClickListener { onDelete(g) }
    }

    override fun getItemCount(): Int = goals.size

    fun submitList(list: List<Goal>) {
        goals = list
        notifyDataSetChanged()
    }
}
