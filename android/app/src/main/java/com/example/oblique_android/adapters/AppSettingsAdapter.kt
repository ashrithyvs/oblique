package com.example.oblique_android.adapters

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

class AppsSettingsAdapter(
    private var list: List<AppInfo>,
    private val selected: MutableSet<String>,
    private val onSelectionChanged: (pkg: String, checked: Boolean) -> Unit
) : RecyclerView.Adapter<AppsSettingsAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.ivAppIcon)
        val tvLabel: TextView = v.findViewById(R.id.tvAppLabel)
        val cb: CheckBox = v.findViewById(R.id.cbApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_checkbox, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = list[position]
        holder.tvLabel.text = app.label
        holder.ivIcon.setImageDrawable(app.icon ?: holder.itemView.context.getDrawable(R.drawable.ic_launcher_foreground))
        holder.cb.isChecked = selected.contains(app.packageName)

        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.setOnCheckedChangeListener { _, isChecked ->
            onSelectionChanged(app.packageName, isChecked)
        }

        holder.itemView.setOnClickListener {
            val newChecked = !holder.cb.isChecked
            holder.cb.isChecked = newChecked
            onSelectionChanged(app.packageName, newChecked)
        }
    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<AppInfo>) {
        list = newList
        notifyDataSetChanged()
    }
}
