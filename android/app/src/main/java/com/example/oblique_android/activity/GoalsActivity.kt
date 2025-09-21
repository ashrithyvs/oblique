package com.example.oblique_android.activity

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oblique_android.R
import com.example.oblique_android.adapters.GoalTypeAdapter
import com.example.oblique_android.adapters.GoalsAdapter
import com.example.oblique_android.adapters.PlatformsAdapter
import com.example.oblique_android.models.GoalType
import com.example.oblique_android.models.GoalsViewModel
import com.example.oblique_android.network.api.GoalRequest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.utils.FlowDecider

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
    private lateinit var emptyStateCard: View
    private lateinit var platformsAdapter: PlatformsAdapter
    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var goalTypeAdapter: GoalTypeAdapter
    private lateinit var vm: GoalsViewModel
    private var selectedPlatform: String? = null
    private var selectedGoalType: GoalType? = null
    private lateinit var proTipCard: CardView

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
        emptyStateCard = findViewById(R.id.emptyStateCard)
        proTipCard = findViewById(R.id.proTipCard)

        vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application as Application)
        ).get(GoalsViewModel::class.java)

        rvPlatforms.layoutManager = GridLayoutManager(this, 2)
        platformsAdapter = PlatformsAdapter(listener = this)
        rvPlatforms.adapter = platformsAdapter

        val goalTypesList = listOf(
            GoalType("lessons", "Lessons", "Complete lessons", "lessons", 1),
            GoalType("minutes", "Focused time", "Minutes spent", "minutes", 10),
            GoalType("pages", "Reading", "Pages read", "pages", 5)
        )
        goalTypeAdapter = GoalTypeAdapter(goalTypesList) { onGoalTypeSelected(it) }
        rvGoalTypes.layoutManager = LinearLayoutManager(this)
        rvGoalTypes.adapter = goalTypeAdapter

        goalsAdapter = GoalsAdapter(
            onDelete = { goal -> lifecycleScope.launch { vm.deleteGoal(goal.id) } },
            onVerify = { goal -> lifecycleScope.launch { vm.completeGoal(goal.id) } }
        )
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalsAdapter

        vm.allGoals.observe(this) { list ->
            goalsAdapter.submitList(list)
            emptyStateCard.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            rvGoals.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            proTipCard.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            updateBottomCTA(list.size)
        }

        btnAddGoal.setOnClickListener {
            val platform = selectedPlatform ?: return@setOnClickListener toast("Pick a platform")
            val gt = selectedGoalType ?: return@setOnClickListener toast("Pick a goal type")
            val target = etTarget.text.toString().trim().toIntOrNull()
                ?: return@setOnClickListener toast("Enter a valid target")

            val req = GoalRequest(
                platform = platform,
                platformUsername = "",
                unit = gt.unit,
                targetValue = target,
                baselineValue = 0,
                deadline = null,
                title = "${gt.title} on $platform",
                checkIntervalMs = 3600000L
            )

            lifecycleScope.launch {
                vm.createGoal(req) { created ->
                    if (created != null) toast("Goal added") else toast("Failed to add goal")
                }
            }
            clearSelectionAfterAdd()
        }

        btnStart.setOnClickListener {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(PREF_GOALS_SHOWN, true).apply()
            val next = FlowDecider.nextActivity(this)
            startActivity(Intent(this, next))
            finish()
        }

        cardSelectedPlatform.visibility = View.GONE
    }

    private fun onGoalTypeSelected(goalType: GoalType) {
        selectedGoalType = goalType
        tvTargetLabel.text = "Target (${goalType.unit})"
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
        if (platform.isEmpty()) {
            selectedPlatform = null
            cardSelectedPlatform.visibility = View.GONE
            tvSelectedPlatformName.text = ""
        } else {
            selectedPlatform = platform
            tvSelectedPlatformName.text = platform
            cardSelectedPlatform.visibility = View.VISIBLE
            scrollView.post { scrollView.smoothScrollTo(0, cardSelectedPlatform.top) }
        }
    }

    private fun updateBottomCTA(totalGoals: Int) {
        btnStart.apply {
            isEnabled = totalGoals > 0
            alpha = if (totalGoals > 0) 1f else 0.6f
            text = if (totalGoals == 0) {
                "Add at least one goal to continue"
            } else {
                "Start with $totalGoals goal${if (totalGoals > 1) "s" else ""}"
            }
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }
}
