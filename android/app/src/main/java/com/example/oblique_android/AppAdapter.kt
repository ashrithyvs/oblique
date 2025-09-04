package com.example.oblique_android

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: List<ApplicationInfo>,
    private val onCheckedChange: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private val selectedApps = mutableSetOf<String>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tvAppName)
        val checkBox: CheckBox = view.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.loadLabel(holder.itemView.context.packageManager)
        holder.checkBox.isChecked = selectedApps.contains(app.packageName)
        holder.checkBox.setOnCheckedChangeListener { _, _ ->
            if (selectedApps.contains(app.packageName)) selectedApps.remove(app.packageName)
            else selectedApps.add(app.packageName)
            onCheckedChange(app)
        }
    }

    override fun getItemCount(): Int = apps.size
}
