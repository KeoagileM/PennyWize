package com.pennywize.app.model

/**
 * Represents a single achievement badge.
 *
 * @param id          Unique string key
 * @param title       Short display name
 * @param description What the user did to earn it
 * @param icon        Emoji shown on the badge
 * @param isUnlocked  Whether the user has earned it yet
 * @param tier        "bronze" | "silver" | "gold" — controls badge colour
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val tier: String = "bronze"   // bronze | silver | gold
)

/**
 * Central registry of every achievement the app can award.
 * Pass in live counts / flags and get back a fully-evaluated list.
 */
object AchievementManager {

    fun evaluate(
        categoryCount: Int,
        expenseCount: Int,
        totalSpent: Double,
        minGoal: Double?,
        maxGoal: Double?
    ): List<Achievement> {

        val list = mutableListOf<Achievement>()

        // ── Category achievements ─────────────────────────────────────────
        list += Achievement(
            id          = "cat_first",
            title       = "Organiser",
            description = "Created your first category",
            icon        = "📁",
            isUnlocked  = categoryCount >= 1,
            tier        = "bronze"
        )
        list += Achievement(
            id          = "cat_five",
            title       = "Neat Freak",
            description = "Created 5 categories",
            icon        = "🗂️",
            isUnlocked  = categoryCount >= 5,
            tier        = "silver"
        )
        list += Achievement(
            id          = "cat_ten",
            title       = "Master Planner",
            description = "Created 10 categories",
            icon        = "🏗️",
            isUnlocked  = categoryCount >= 10,
            tier        = "gold"
        )

        // ── Expense achievements ──────────────────────────────────────────
        list += Achievement(
            id          = "exp_first",
            title       = "First Spend",
            description = "Logged your first expense",
            icon        = "💸",
            isUnlocked  = expenseCount >= 1,
            tier        = "bronze"
        )
        list += Achievement(
            id          = "exp_ten",
            title       = "Tracking Pro",
            description = "Logged 10 expenses",
            icon        = "📊",
            isUnlocked  = expenseCount >= 10,
            tier        = "bronze"
        )
        list += Achievement(
            id          = "exp_twentyfive",
            title       = "Expense Hawk",
            description = "Logged 25 expenses",
            icon        = "🦅",
            isUnlocked  = expenseCount >= 25,
            tier        = "silver"
        )
        list += Achievement(
            id          = "exp_fifty",
            title       = "Penny Counter",
            description = "Logged 50 expenses",
            icon        = "🪙",
            isUnlocked  = expenseCount >= 50,
            tier        = "silver"
        )
        list += Achievement(
            id          = "exp_hundred",
            title       = "Ledger Legend",
            description = "Logged 100 expenses",
            icon        = "📒",
            isUnlocked  = expenseCount >= 100,
            tier        = "gold"
        )

        // ── Goal achievements ─────────────────────────────────────────────
        // Min goal surpassed = user spent MORE than the minimum target
        val minSurpassed = minGoal != null && minGoal > 0 && totalSpent >= minGoal
        list += Achievement(
            id          = "goal_min_reached",
            title       = "Budget Active",
            description = "Spending has reached your minimum goal",
            icon        = "🎯",
            isUnlocked  = minSurpassed,
            tier        = "bronze"
        )

        // Max goal surpassed = user went OVER budget
        val maxSurpassed = maxGoal != null && maxGoal > 0 && totalSpent > maxGoal
        list += Achievement(
            id          = "goal_max_exceeded",
            title       = "Over Budget",
            description = "You exceeded your maximum spending goal",
            icon        = "🚨",
            isUnlocked  = maxSurpassed,
            tier        = "silver"   // silver so it stands out as a warning
        )

        // Stayed under max — reward good behaviour
        val stayedUnder = maxGoal != null && maxGoal > 0 &&
                totalSpent > 0 && totalSpent <= maxGoal
        list += Achievement(
            id          = "goal_under_budget",
            title       = "Budget Master",
            description = "Stayed within your maximum budget this month",
            icon        = "✅",
            isUnlocked  = stayedUnder,
            tier        = "gold"
        )

        // Spent nothing (great saver)
        list += Achievement(
            id          = "zero_spend",
            title       = "Iron Saver",
            description = "No expenses logged this month — incredible discipline!",
            icon        = "🧊",
            isUnlocked  = expenseCount == 0 && (minGoal != null),
            tier        = "gold"
        )

        return list
    }
}