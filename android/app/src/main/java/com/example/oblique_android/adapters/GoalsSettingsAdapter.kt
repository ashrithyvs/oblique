package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.models.Goal

class GoalsSettingsAdapter(
    private var items: List<Goal>,
    private val onEdit: (Goal) -> Unit,
    private val onDelete: (Goal) -> Unit
) : RecyclerView.Adapter<GoalsSettingsAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
        val tvTitle: TextView = v.findViewById(R.id.tvGoalPlatform)
        val tvSubtitle: TextView = v.findViewById(R.id.tvGoalSubtitle)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_settings, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = items[position]
        holder.tvTitle.text = g.platform
        holder.tvSubtitle.text = "${g.targetValue} ${g.unit} daily"

        // choose icon by platform (fallback)
        val ctx = holder.itemView.context
        val res = when (g.platform) {
            "LeetCode" -> R.drawable.ic_leetcode
            "Duolingo" -> R.drawable.ic_duolingo
            else -> R.drawable.ic_launcher_foreground
        }
        holder.ivIcon.setImageResource(res)

        holder.btnEdit.setOnClickListener { onEdit(g) }
        holder.btnDelete.setOnClickListener { onDelete(g) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<Goal>) {
        this.items = list
        notifyDataSetChanged()
    }
}
