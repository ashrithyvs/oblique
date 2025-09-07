package com.example.oblique_android

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val selectedApps: MutableSet<String>,
    private val onSelectionChanged: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()

    fun setOriginalList(list: List<AppInfo>) {
        Log.d("AppAdapter", "Original list set with ${list.size} apps")
        originalApps.clear()
        originalApps.addAll(list)
    }

    fun updateList(newList: List<AppInfo>) {
        Log.d("AppAdapter", "Updating list with ${newList.size} apps")
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val lower = query.lowercase()
        val filtered = if (lower.isEmpty()) {
            originalApps
        } else {
            originalApps.filter {
                it.name.lowercase().contains(lower) ||
                        it.packageName.lowercase().contains(lower)
            }
        }
        Log.d("AppAdapter", "Filter applied: '$query' -> ${filtered.size} results")
        updateList(filtered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(v)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, selectedApps.contains(app.packageName)) {
            onSelectionChanged(app)
            Log.d("AppAdapter", "Selection changed: ${app.packageName}, selected=${selectedApps.contains(app.packageName)}")
        }
    }

    class AppViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val icon: ImageView = v.findViewById(R.id.ivAppIcon)
        private val name: TextView = v.findViewById(R.id.tvAppName)
        private val pkg: TextView = v.findViewById(R.id.tvPackage)
        private val checkBox: CheckBox = v.findViewById(R.id.cbSelect)

        fun bind(app: AppInfo, isSelected: Boolean, onSelectionChanged: () -> Unit) {
            icon.setImageDrawable(app.icon)
            name.text = app.name
            pkg.text = app.packageName
            checkBox.isChecked = isSelected

            // Make row click toggle only the checkbox
            itemView.setOnClickListener {
                checkBox.toggle()
            }

            // Handle actual selection updates here
            checkBox.setOnCheckedChangeListener { _, checked ->
                Log.d("AppViewHolder", "Checkbox changed for ${app.packageName}: $checked")
                onSelectionChanged()
            }
        }
    }
}

