package com.example.oblique_android.activity

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
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
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.setupWindowInsets
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class GoalsActivity : AppCompatActivity(), PlatformsAdapter.PlatformClickListener {

    private lateinit var rvPlatforms: RecyclerView
    private lateinit var rvGoalTypes: RecyclerView
    private lateinit var rvGoals: RecyclerView
    private lateinit var etTarget: EditText
    private lateinit var btnAddGoal: Button
    private lateinit var btnStart: Button
    private lateinit var cardSelectedPlatform: View
    private lateinit var tvSelectedPlatformName: TextView
    private lateinit var ivSelectedIcon: ImageView
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
    private lateinit var btnPickDeadline: Button
    private lateinit var tvDeadlinePreview: TextView

    private var selectedDeadlineMsOfDay: Long = -1L
    private var selectedDeadlineEpoch: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)
        setupWindowInsets(R.id.scrollView)

        rvPlatforms = findViewById(R.id.rvPlatforms)
        rvGoalTypes = findViewById(R.id.rvGoalTypes)
        rvGoals = findViewById(R.id.rvGoals)
        etTarget = findViewById(R.id.etTarget)
        btnAddGoal = findViewById(R.id.btnAddGoal)
        btnStart = findViewById(R.id.btnStart)
        cardSelectedPlatform = findViewById(R.id.cardSelectedPlatform)
        tvSelectedPlatformName = findViewById(R.id.tvSelectedPlatformName)
        ivSelectedIcon = findViewById(R.id.ivSelectedIcon)
        scrollView = findViewById(R.id.scrollView)
        tvTargetLabel = findViewById(R.id.tvTargetLabel)
        emptyStateCard = findViewById(R.id.emptyStateCard)
        proTipCard = findViewById(R.id.proTipCard)
        btnPickDeadline = findViewById(R.id.btnPickDeadline)
        tvDeadlinePreview = findViewById(R.id.tvDeadlinePreview)

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
        btnPickDeadline.setOnClickListener {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)

            val picker = MaterialTimePicker.Builder()
                .setTitleText("Select goal deadline")
                .setHour(hour)
                .setMinute(minute)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build()

            picker.addOnPositiveButtonClickListener {
                val selectedHour = picker.hour
                val selectedMinute = picker.minute
                val display = String.format("%02d:%02d %s",
                    if (selectedHour % 12 == 0) 12 else selectedHour % 12,
                    selectedMinute,
                    if (selectedHour >= 12) "PM" else "AM"
                )
                tvDeadlinePreview.text = "Deadline: $display"

                // compute ms since midnight
                selectedDeadlineMsOfDay = (selectedHour * 60 * 60 * 1000L) + (selectedMinute * 60 * 1000L)

                // compute absolute epoch (today + selected time)
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                selectedDeadlineEpoch = todayStart + selectedDeadlineMsOfDay

                Log.i("GoalsActivity", "Deadline selected: $display")
                Log.i("GoalsActivity", "→ sinceMidnight=$selectedDeadlineMsOfDay, absolute=$selectedDeadlineEpoch")
            }

            picker.show(supportFragmentManager, "deadline_picker")
        }

        btnAddGoal.setOnClickListener {
            val platform = selectedPlatform ?: return@setOnClickListener toast("Pick a platform")
            val gt = selectedGoalType ?: return@setOnClickListener toast("Pick a goal type")
            val target = etTarget.text.toString().trim().toIntOrNull()
                ?: return@setOnClickListener toast("Enter a valid target")

            if (selectedDeadlineEpoch < 0) {
                toast("Please select a deadline time")
                return@setOnClickListener
            }

            val platformKey = platform.lowercase()
            val username = PrefsUtils.getPlatformUsername(this, platformKey).orEmpty()

            val req = GoalRequest(
                platform = platform,
                platformUsername = username,
                unit = gt.unit,
                targetValue = target,
                baselineValue = 0,
                deadline = selectedDeadlineEpoch, // ✅ using absolute timestamp (Option A)
                title = "${gt.title} on $platform",
                checkIntervalMs = 3600000L
            )

            lifecycleScope.launch {
                vm.createGoal(req) { created ->
                    if (created != null) {
                        toast("Goal added")
                    } else {
                        toast("Goal creation failed. Please retry.")
                    }
                }
            }
            clearSelectionAfterAdd()
        }

        btnStart.setOnClickListener {
            val next = OnboardingRouter.next(this)
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
        platformsAdapter.clearSelection()
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
            ivSelectedIcon.setImageResource(platformIconRes(platform))
            cardSelectedPlatform.visibility = View.VISIBLE
            scrollView.post { scrollView.smoothScrollTo(0, cardSelectedPlatform.top) }
        }
    }

    private fun platformIconRes(platform: String): Int = when (platform.lowercase()) {
        "leetcode" -> R.drawable.ic_leetcode
        "duolingo" -> R.drawable.ic_duolingo
        else -> R.drawable.ic_placeholder
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
