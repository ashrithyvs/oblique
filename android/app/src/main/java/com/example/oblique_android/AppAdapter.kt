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

    private val apps: MutableList<AppInfo> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = apps.size

    fun updateList(newList: List<AppInfo>) {
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
        Log.d("AppAdapter", "Adapter updated, size = ${apps.size}")
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvPackage: TextView = itemView.findViewById(R.id.tvPackage)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(app: AppInfo) {
            ivAppIcon.setImageDrawable(app.icon)
            tvAppName.text = app.name
            tvPackage.text = app.packageName
            cbSelect.isChecked = selectedApps.contains(app.packageName)

            // Avoid duplicate triggers
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                toggleSelection(app, isChecked)
            }

            itemView.setOnClickListener {
                val newState = !cbSelect.isChecked
                cbSelect.isChecked = newState
                toggleSelection(app, newState)
            }
        }

        private fun toggleSelection(app: AppInfo, isChecked: Boolean) {
            if (isChecked) {
                selectedApps.add(app.packageName)
            } else {
                selectedApps.remove(app.packageName)
            }
            onSelectionChanged(app)
        }
    }
}
