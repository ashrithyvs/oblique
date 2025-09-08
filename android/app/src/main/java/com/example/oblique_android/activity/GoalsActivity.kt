package com.example.oblique_android.activity

import android.app.Application
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
import com.example.oblique_android.GoalsViewModel
import com.example.oblique_android.PlatformsAdapter
import com.example.oblique_android.R
import com.example.oblique_android.adapters.GoalTypeAdapter
import com.example.oblique_android.adapters.GoalsAdapter
import com.example.oblique_android.models.Goal
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

    private var selectedPlatform: String? = null
    private var selectedGoalType: GoalTypeAdapter.GoalType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        // find views (no viewBinding required)
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

        // ViewModel: use normal provider to avoid custom factory mismatch
        vm = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application as Application))
            .get(GoalsViewModel::class.java)

        // Platforms grid
        rvPlatforms.layoutManager = GridLayoutManager(this, 2)
        val platformsAdapter = PlatformsAdapter(this)
        rvPlatforms.adapter = platformsAdapter

        // Goal types
        goalTypeAdapter = GoalTypeAdapter { goalType ->
            onGoalTypeSelected(goalType)
        }
        rvGoalTypes.layoutManager = LinearLayoutManager(this)
        rvGoalTypes.adapter = goalTypeAdapter

        // Goals list
        goalsAdapter = GoalsAdapter(onDelete = { goal ->
            // PASS the Goal object (not id) to match repository signature
            lifecycleScope.launch { vm.deleteGoal(goal) }
        }, onVerify = { goal ->
            lifecycleScope.launch { vm.markGoalComplete(goal) }
        })
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        // Observe goals — prefer LiveData if exists, otherwise Flow:
        // Try LiveData field "allGoals" first (many repos expose this), fallback to Flow if present.
        try {
            // If your ViewModel provides LiveData<List<Goal>> named `allGoals`, use this:
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
            // Fallback: check for Flow method named getAllGoalsFlow()
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
                } catch (_: Exception) {
                    // If neither exists, skip; you may wire observing manually depending on your VM API
                }
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
            if (targetText.isEmpty()) {
                toast("Enter target")
                return@setOnClickListener
            }
            val target = targetText.toIntOrNull()
            if (target == null || target <= 0) {
                toast("Invalid target number")
                return@setOnClickListener
            }

            val goal = Goal(
                platform = platform,
                unit = gt.unit,
                targetValue = target,
                progress = 0
            )
            lifecycleScope.launch {
                vm.addGoal(goal)
                clearSelectionAfterAdd()
                toast("Goal added")
            }
        }

        btnStart.setOnClickListener {
            finish()
        }

        // Seed if VM has a seeding method (if not present this try/catch is safe)
        lifecycleScope.launch {
            try {
                val seedMethod = vm::class.java.methods.firstOrNull { it.name.contains("seed") || it.name.contains("Seed") }
                seedMethod?.invoke(vm)
            } catch (_: Exception) { /* ignore */ }
        }

        // Hide selected card until platform chosen
        cardSelectedPlatform.visibility = View.GONE
    }

    private fun onGoalTypeSelected(goalType: GoalTypeAdapter.GoalType) {
        selectedGoalType = goalType
        tvTargetLabel.text = when (goalType.unit) {
            "minutes" -> "Target (minutes)"
            "pages" -> "Target (pages)"
            else -> "Target (${goalType.unit})"
        }
        etTarget.hint = goalType.suggested.toString()
        etTarget.inputType = InputType.TYPE_CLASS_NUMBER
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
        // scroll to selected platform
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
