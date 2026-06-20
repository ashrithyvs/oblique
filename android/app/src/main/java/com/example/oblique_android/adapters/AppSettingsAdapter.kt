package com.example.oblique_android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.models.AppInfo

class AppsSettingsAdapter(
    private var apps: List<AppInfo>,
    private var blockedSet: MutableSet<String>,
    private val onToggle: (pkg: String, checked: Boolean) -> Unit
) : RecyclerView.Adapter<AppsSettingsAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_checkbox, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    fun updateBlockedApps(newBlocked: Set<String>) {
        blockedSet.clear()
        blockedSet.addAll(newBlocked)
        notifyDataSetChanged()
    }

    fun submitList(newList: List<AppInfo>) {
        apps = newList
        notifyDataSetChanged()
    }

    fun getSelectedPackages(): List<String> = blockedSet.toList()

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv = itemView.findViewById<ImageView>(R.id.ivAppIcon)
        private val tv = itemView.findViewById<TextView>(R.id.tvAppLabel)
        private val cb = itemView.findViewById<CheckBox>(R.id.cbApp)
        private var currentPkg: String? = null

        fun bind(app: AppInfo) {
            currentPkg = app.packageName
            iv.setImageDrawable(app.icon)
            tv.text = app.name

            cb.setOnCheckedChangeListener(null)
            cb.isChecked = blockedSet.contains(app.packageName)

            cb.setOnCheckedChangeListener { _, checked ->
                currentPkg?.let { pkg ->
                    if (checked) blockedSet.add(pkg) else blockedSet.remove(pkg)
                    onToggle(pkg, checked)
                }
            }
        }
    }
}
