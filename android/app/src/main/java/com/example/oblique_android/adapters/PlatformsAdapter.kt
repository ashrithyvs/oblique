package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R

/**
 * Simple adapter for the platform selection grid used in GoalsActivity.
 * Provide a listener to receive platform selection callbacks.
 */
class PlatformsAdapter(
    private val platforms: List<PlatformItem> = defaultPlatforms(),
    private val listener: PlatformClickListener
) : RecyclerView.Adapter<PlatformsAdapter.VH>() {

    interface PlatformClickListener {
        fun onPlatformSelected(platform: String)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardPlatform)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivPlatformIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvPlatformName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // IMPORTANT: inflate the item layout (not activity layout) and use that view for findViewById
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_platform, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = platforms[position]
        holder.tvName.text = p.name
        // use placeholder icons from drawable resource if available
        holder.ivIcon.setImageResource(p.iconRes)
        holder.card.setOnClickListener {
            listener.onPlatformSelected(p.name)
        }
    }

    override fun getItemCount(): Int = platforms.size

    data class PlatformItem(val name: String, val iconRes: Int)

    companion object {
        // small default list so adapter can be reused. Replace iconRes with actual drawables.
        private fun defaultPlatforms() = listOf(
            PlatformItem("LeetCode", R.drawable.ic_leetcode),
            PlatformItem("Duolingo", R.drawable.ic_duolingo),
        )
    }
}
