package com.example.oblique_android.adapters

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.prefs.PlatformPref

class PlatformPrefsAdapter(
    private val items: List<PlatformPref>,
    private val prefs: SharedPreferences
) : RecyclerView.Adapter<PlatformPrefsAdapter.PlatformViewHolder>() {

    // keep references to the EditTexts so we can read values later
    private val inputs = mutableMapOf<String, EditText>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_platform_username, parent, false)
        return PlatformViewHolder(v)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.displayName
        holder.icon.setImageResource(item.iconRes)

        // Prefill from SharedPreferences (if exists)
        holder.username.setText(prefs.getString(item.key, ""))

        // keep reference so we can fetch later
        inputs[item.key] = holder.username
    }

    override fun getItemCount() = items.size

    /**
     * Return current username map keyed by platform key.
     * Only non-empty trimmed values are included.
     */
    fun getUsernames(): java.util.Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for ((key, et) in inputs) {
            val value = et.text.toString().trim()
            if (value.isNotEmpty()) {
                map[key] = value
            }
        }
        return map as java.util.Map<String, String> // ✅ explicit upcast fixes type mismatch
    }

    /**
     * Persist current usernames into SharedPreferences.Editor.
     * Note: does not call apply() so caller can batch with other edits.
     */
    fun saveUsernames(editor: SharedPreferences.Editor) {
        for ((key, et) in inputs) {
            editor.putString(key, et.text.toString())
        }
    }

    inner class PlatformViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.platformIcon)
        val title: TextView = v.findViewById(R.id.platformTitle)
        val username: EditText = v.findViewById(R.id.platformUsername)
    }
}
