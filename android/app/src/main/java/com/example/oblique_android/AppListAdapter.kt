package com.example.oblique_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val context: Context,
    private val appList: List<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.appIcon)
        val name: TextView = itemView.findViewById(R.id.appName)
        val checkBox: CheckBox = itemView.findViewById(R.id.appCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app_tile, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.checkBox.isChecked = app.isSelected

        holder.itemView.setOnClickListener {
            app.isSelected = !app.isSelected
            holder.checkBox.isChecked = app.isSelected
        }

        holder.checkBox.setOnClickListener {
            app.isSelected = holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = appList.size
}
