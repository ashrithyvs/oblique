package com.example.oblique_android.activity

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.adapters.PlatformsAdapter
import com.example.oblique_android.adapters.GoalTypeAdapter
import com.example.oblique_android.adapters.GoalsAdapter
import com.example.oblique_android.models.Goal
import com.example.oblique_android.models.GoalType
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.R
import kotlinx.coroutines.launch

class GoalsActivity : ComponentActivity(), PlatformsAdapter.PlatformClickListener {

    private lateinit var rvPlatforms: RecyclerView
    private lateinit var rvGoalTypes: RecyclerView
    private lateinit var rvGoals: RecyclerView
    private lateinit var etTarget: EditText
    private lateinit var btnAddGoal: Button
    private lateinit var btnStart: Button
    private lateinit var cardSelectedPlatform: View
    private lateinit var tvSelectedPlatformName: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var tvTargetLabel: TextView

    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var goalTypeAdapter: GoalTypeAdapter
    private lateinit var vm: GoalsViewModel

    // ← CHANGED: selectedGoalType should hold the GoalType object, not a String
    private var selectedPlatform: String? = null
    private var selectedGoalType: GoalType? = null

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val PREF_GOALS_SHOWN = "goals_shown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        rvPlatforms = findViewById(R.id.rvPlatforms)
        rvGoalTypes = findViewById(R.id.rvGoalTypes)
        rvGoals = findViewById(R.id.rvGoals)
        etTarget = findViewById(R.id.etTarget)
        btnAddGoal = findViewById(R.id.btnAddGoal)
        btnStart = findViewById(R.id.btnStart)
        cardSelectedPlatform = findViewById(R.id.cardSelectedPlatform)
        tvSelectedPlatformName = findViewById(R.id.tvSelectedPlatformName)
        scrollView = findViewById(R.id.scrollView)
        tvTargetLabel = findViewById(R.id.tvTargetLabel)

        vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application as Application)
        ).get(GoalsViewModel::class.java)

        // Platforms grid
        rvPlatforms.layoutManager = GridLayoutManager(this, 2)
        val platformsAdapter = PlatformsAdapter(listener = this) // ✅ FIX
        rvPlatforms.adapter = platformsAdapter

        // ---------- FIX: create a concrete list of GoalType items and pass into adapter ----------
        // Use your models.GoalType (you already provided that data class)
        val goalTypesList = listOf(
            GoalType(id = "lessons", title = "Lessons", subtitle = "Complete lessons", unit = "lessons", suggested = 1),
            GoalType(id = "minutes", title = "Focused time", subtitle = "Minutes spent", unit = "minutes", suggested = 10),
            GoalType(id = "pages", title = "Reading", subtitle = "Pages read", unit = "pages", suggested = 5)
        )

        // Goal types — pass items list + click lambda
        goalTypeAdapter = GoalTypeAdapter(items = goalTypesList) { goalType -> onGoalTypeSelected(goalType) }
        rvGoalTypes.layoutManager = LinearLayoutManager(this)
        rvGoalTypes.adapter = goalTypeAdapter
        // --------------------------------------------------------------------------

        // Goals list
        goalsAdapter = GoalsAdapter(
            onDelete = { goal -> lifecycleScope.launch { vm.deleteGoal(goal) } },
            onVerify = { goal -> lifecycleScope.launch { vm.markGoalComplete(goal) } }
        )
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        // Observe goals (same as you had)
        try {
            val liveDataField = vm::class.java.getDeclaredField("allGoals")
            @Suppress("UNCHECKED_CAST")
            val ld = liveDataField.get(vm) as? androidx.lifecycle.LiveData<List<Goal>>
            ld?.observe(this) { list ->
                goalsAdapter.submitList(list)
                findViewById<View>(R.id.emptyStateCard).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                rvGoals.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                updateBottomCTA(list.size)
            }
        } catch (_: Exception) {
            lifecycleScope.launch {
                try {
                    val method = vm::class.java.methods.firstOrNull { it.name.contains("getAll") && it.returnType.name.contains("Flow") }
                    if (method != null) {
                        @Suppress("UNCHECKED_CAST")
                        val flow = method.invoke(vm) as kotlinx.coroutines.flow.Flow<List<Goal>>
                        flow.collect { list ->
                            runOnUiThread {
                                goalsAdapter.submitList(list)
                                findViewById<View>(R.id.emptyStateCard).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                                rvGoals.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                                updateBottomCTA(list.size)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        btnAddGoal.setOnClickListener {
            val platform = selectedPlatform ?: run {
                toast("Pick a platform")
                return@setOnClickListener
            }
            val gt = selectedGoalType ?: run {
                toast("Pick a goal type")
                return@setOnClickListener
            }

            val targetText = etTarget.text.toString().trim()
            val target = targetText.toIntOrNull()
            if (target == null || target <= 0) {
                toast("Enter a valid target")
                return@setOnClickListener
            }

            // ---------- FIX: now gt is a GoalType, so gt.unit is valid ----------
            val goal = Goal(platform = platform, unit = gt.unit, targetValue = target, progress = 0)
            lifecycleScope.launch {
                vm.addGoal(goal)
                clearSelectionAfterAdd()
                toast("Goal added")
            }
        }

        btnStart.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_GOALS_SHOWN, true).apply()

            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        lifecycleScope.launch {
            try {
                val seedMethod = vm::class.java.methods.firstOrNull { it.name.contains("seed", true) }
                seedMethod?.invoke(vm)
            } catch (_: Exception) { }
        }

        cardSelectedPlatform.visibility = View.GONE
    }

    // Accept models.GoalType so selectedGoalType and usage are consistent
    private fun onGoalTypeSelected(goalType: GoalType) {
        selectedGoalType = goalType
        tvTargetLabel.text = when (goalType.unit) {
            "minutes" -> "Target (minutes)"
            "pages" -> "Target (pages)"
            else -> "Target (${goalType.unit})"
        }
        etTarget.hint = goalType.suggested.toString()
        etTarget.inputType = InputType.TYPE_CLASS_NUMBER
        // adapter.select expects the adapter's GoalType type — this assumes adapter uses same model type
        goalTypeAdapter.select(goalType)
    }

    private fun clearSelectionAfterAdd() {
        selectedGoalType = null
        selectedPlatform = null
        goalTypeAdapter.clearSelection()
        etTarget.setText("")
        cardSelectedPlatform.visibility = View.GONE
    }

    override fun onPlatformSelected(platform: String) {
        selectedPlatform = platform
        tvSelectedPlatformName.text = platform
        cardSelectedPlatform.visibility = View.VISIBLE
        scrollView.post { scrollView.smoothScrollTo(0, cardSelectedPlatform.top) }
    }

    private fun updateBottomCTA(totalGoals: Int) {
        if (totalGoals == 0) {
            btnStart.text = "Add at least one goal to continue"
            btnStart.isEnabled = false
            btnStart.alpha = 0.6f
        } else {
            btnStart.text = if (totalGoals == 1) "Start with 1 goal" else "Start with $totalGoals goals"
            btnStart.isEnabled = true
            btnStart.alpha = 1f
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }
}
