package com.example.oblique_android.ui.preferences

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.PlatformPrefsAdapter
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.network.request.UserPreferencesRequest
import com.example.oblique_android.prefs.PlatformPref
import com.example.oblique_android.utils.PlatformCatalog
import com.example.oblique_android.utils.PrefsUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlatformPreferencesFragment : Fragment() {

    interface Listener {
        fun onPreferencesActionStarted()
        fun onPreferencesSaved(displayName: String, usernames: Map<String, String>)
        fun onPreferencesActionFailed(message: String)
    }

    private lateinit var etDisplayName: EditText
    private lateinit var tvPlatformSubtitle: TextView
    private lateinit var rvPlatforms: RecyclerView
    private lateinit var btnSave: MaterialButton
    private lateinit var sliderDeadlineBuffer: Slider
    private lateinit var tvDeadlineBufferValue: TextView
    private lateinit var adapter: PlatformPrefsAdapter

    private var listener: Listener? = null
    private var mode: String = MODE_SETTINGS
    private var platformFilter: Set<String>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = arguments?.getString(ARG_MODE) ?: MODE_SETTINGS
        platformFilter = arguments?.getStringArrayList(ARG_PLATFORM_FILTER)?.toSet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_platform_preferences, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etDisplayName = view.findViewById(R.id.etDisplayName)
        tvPlatformSubtitle = view.findViewById(R.id.tvPlatformSubtitle)
        rvPlatforms = view.findViewById(R.id.rvPlatforms)
        btnSave = view.findViewById(R.id.btnSave)
        sliderDeadlineBuffer = view.findViewById(R.id.sliderDeadlineBuffer)
        tvDeadlineBufferValue = view.findViewById(R.id.tvDeadlineBufferValue)

        etDisplayName.setText(PrefsUtils.getDisplayName(requireContext()).orEmpty())
        bindDeadlineBuffer(PrefsUtils.getDeadlineBufferMs(requireContext()))
        sliderDeadlineBuffer.addOnChangeListener { _, value, _ ->
            tvDeadlineBufferValue.text = getString(R.string.deadline_buffer_value, value.toInt())
        }

        val allPlatforms = PlatformCatalog.all.map {
            PlatformPref(it.key, it.displayName, it.iconRes)
        }
        val platforms = platformFilter?.let { filter ->
            allPlatforms.filter { it.key in filter }
        } ?: allPlatforms

        adapter = PlatformPrefsAdapter(requireContext(), platforms)
        rvPlatforms.layoutManager = LinearLayoutManager(requireContext())
        rvPlatforms.adapter = adapter

        if (mode == MODE_ONBOARDING) {
            btnSave.setText(R.string.continue_setup)
            tvPlatformSubtitle.setText(R.string.platform_usernames_required)
        } else {
            btnSave.setText(R.string.save_settings)
            tvPlatformSubtitle.setText(R.string.platform_usernames_optional)
        }

        btnSave.setOnClickListener { handleSave() }

        if (mode == MODE_SETTINGS) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = ApiClient.getClient(requireContext())
                        .create(UserApi::class.java)
                        .getCurrentUser()
                    user.deadlineBufferMs?.let {
                        PrefsUtils.saveDeadlineBufferMs(requireContext(), it)
                        bindDeadlineBuffer(it)
                    }
                } catch (_: Exception) {
                    // keep cached value
                }
            }
        }
    }

    private fun bindDeadlineBuffer(bufferMs: Long) {
        val hours = (bufferMs / 3_600_000L).toInt().coerceIn(0, 3)
        sliderDeadlineBuffer.value = hours.toFloat()
        tvDeadlineBufferValue.text = getString(R.string.deadline_buffer_value, hours)
    }

    private fun selectedBufferMs(): Long =
        sliderDeadlineBuffer.value.toLong() * 3_600_000L

    private fun handleSave() {
        val displayName = etDisplayName.text.toString().trim()
        val usernames = adapter.getUsernames()

        if (displayName.isEmpty()) {
            Toast.makeText(requireContext(), R.string.display_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (mode == MODE_ONBOARDING) {
            val required = platformFilter.orEmpty()
            val missing = required.filter { usernames[it].isNullOrBlank() }
            if (missing.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.baseline_username_required, missing.first()),
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
        }

        PrefsUtils.saveDisplayName(requireContext(), displayName)
        PrefsUtils.saveDeadlineBufferMs(requireContext(), selectedBufferMs())
        adapter.saveUsernames()
        listener?.onPreferencesActionStarted()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = UserPreferencesRequest(
                    displayName = displayName,
                    usernames = usernames,
                    deadlineBufferMs = selectedBufferMs(),
                )
                val updatedUser = ApiClient.getClient(requireContext())
                    .create(UserApi::class.java)
                    .updatePreferences(request)

                for ((platform, uname) in usernames) {
                    if (uname.isNotBlank()) {
                        PrefsUtils.savePlatformUsername(requireContext(), platform, uname)
                    }
                }
                updatedUser.user.deadlineBufferMs?.let {
                    PrefsUtils.saveDeadlineBufferMs(requireContext(), it)
                }

                withContext(Dispatchers.Main) {
                    if (mode == MODE_SETTINGS) {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.preferences_saved,
                                updatedUser.user.displayName ?: displayName,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    listener?.onPreferencesSaved(displayName, usernames)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener?.onPreferencesActionFailed(
                        getString(R.string.preferences_save_failed),
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_PLATFORM_FILTER = "platform_filter"
        const val MODE_SETTINGS = "settings"
        const val MODE_ONBOARDING = "onboarding"

        fun newInstance(mode: String, platformFilter: List<String>? = null): PlatformPreferencesFragment {
            return PlatformPreferencesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                    if (platformFilter != null) {
                        putStringArrayList(ARG_PLATFORM_FILTER, ArrayList(platformFilter))
                    }
                }
            }
        }
    }
}
