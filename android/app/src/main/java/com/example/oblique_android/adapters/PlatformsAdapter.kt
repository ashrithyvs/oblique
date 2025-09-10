package com.example.oblique_android.adapters

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R

/**
 * Adapter that keeps track of a single selected platform item.
 * Selection uses "activated" state (recommended for RecyclerView selection visuals).
 */
class PlatformsAdapter(
    private val platforms: List<PlatformItem> = defaultPlatforms(),
    private val listener: PlatformClickListener
) : RecyclerView.Adapter<PlatformsAdapter.VH>() {

    interface PlatformClickListener {
        /**
         * Called when a platform is selected.
         * If selection was cleared (toggle-off), an empty string is passed.
         */
        fun onPlatformSelected(platform: String)
    }

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    @SuppressLint("ResourceAsColor")
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardPlatform)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivPlatformIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvPlatformName)

        init {
            // make clickable and attach platform-native ripple foreground
            card.isClickable = true
            val outValue = TypedValue()
            if (itemView.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
                val fg = ContextCompat.getDrawable(itemView.context, outValue.resourceId)
                card.foreground = fg
            }

            card.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                // If same item clicked -> toggle-off behaviour (clear selection)
                if (pos == selectedPosition) {
                    val prev = selectedPosition
                    selectedPosition = RecyclerView.NO_POSITION
                    if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
                    // notify activity selection cleared
                    listener.onPlatformSelected("")
                    card.setBackgroundColor(R.color.bg_white)
                    return@setOnClickListener
                }

                val previous = selectedPosition
                selectedPosition = pos

                if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)

                listener.onPlatformSelected(platforms[pos].name)
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

        // ---- Important: clear and set all relevant states explicitly to avoid recycled-state bugs ----
        holder.card.isActivated = (position == selectedPosition)   // used by your selector drawable
        holder.card.isSelected = (position == selectedPosition)    // defensive: drawable sometimes checks selected
        holder.card.isPressed = false                              // ensure ripple/pressed doesn't stay stuck

        // Ensure inner view(s) don't keep their own state that conflicts
        holder.ivIcon.isActivated = (position == selectedPosition)
        holder.tvName.isActivated = (position == selectedPosition)
    }

    override fun getItemCount(): Int = platforms.size

    /** Return currently selected platform name or null */
    fun getSelectedPlatform(): String? = platforms.getOrNull(selectedPosition)?.name

    /** Programmatically clear selection (if needed) */
    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
    }

    data class PlatformItem(val name: String, val iconRes: Int)

    companion object {
        private fun defaultPlatforms() = listOf(
            PlatformItem("LeetCode", R.drawable.ic_leetcode),
            PlatformItem("Duolingo", R.drawable.ic_duolingo)
            // add more platforms here if you want
        )
    }
}
