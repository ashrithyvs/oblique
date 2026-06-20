package com.example.oblique_android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.prefs.PlatformPref
import com.example.oblique_android.utils.PrefsUtils

class PlatformPrefsAdapter(
    private val context: Context,
    private val items: List<PlatformPref>
) : RecyclerView.Adapter<PlatformPrefsAdapter.PlatformViewHolder>() {

    private val inputs = mutableMapOf<String, EditText>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_platform_username, parent, false)
        return PlatformViewHolder(v)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.displayName
        holder.icon.setImageResource(item.iconRes)

        val saved = PrefsUtils.getPlatformUsername(context, item.key).orEmpty()
        holder.username.setText(saved)
        inputs[item.key] = holder.username
    }

    override fun getItemCount() = items.size

    fun getUsernames(): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for ((key, et) in inputs) {
            val value = et.text.toString().trim()
            if (value.isNotEmpty()) {
                map[key] = value
            }
        }
        return map
    }

    fun saveUsernames() {
        for ((key, et) in inputs) {
            val value = et.text.toString().trim()
            if (value.isNotEmpty()) {
                PrefsUtils.savePlatformUsername(context, key, value)
            }
        }
    }

    inner class PlatformViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.platformIcon)
        val title: TextView = v.findViewById(R.id.platformTitle)
        val username: EditText = v.findViewById(R.id.platformUsername)
    }
}
