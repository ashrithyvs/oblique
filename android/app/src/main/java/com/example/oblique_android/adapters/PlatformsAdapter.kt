package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.utils.PlatformCatalog
import com.example.oblique_android.utils.TileSelectionStyle
import com.google.android.material.card.MaterialCardView

class PlatformsAdapter(
    private val platforms: List<PlatformItem> = PlatformCatalog.all.map {
        PlatformItem(it.displayName, it.iconRes)
    },
    private val listener: PlatformClickListener,
) : RecyclerView.Adapter<PlatformsAdapter.VH>() {

    interface PlatformClickListener {
        fun onPlatformSelected(platform: String)
    }

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardPlatform)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivPlatformIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvPlatformName)

        init {
            card.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                val previous = selectedPosition
                selectedPosition = if (pos == selectedPosition) RecyclerView.NO_POSITION else pos

                if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(selectedPosition)
                    listener.onPlatformSelected(platforms[selectedPosition].name)
                } else {
                    listener.onPlatformSelected("")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_platform, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = platforms[position]
        holder.tvName.text = p.name
        holder.ivIcon.setImageResource(p.iconRes)
        TileSelectionStyle.apply(holder.card, position == selectedPosition, holder.itemView.context)
    }

    override fun getItemCount(): Int = platforms.size

    fun getSelectedPlatform(): String? = platforms.getOrNull(selectedPosition)?.name

    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
    }

    data class PlatformItem(val name: String, val iconRes: Int)
}
