package com.szainabbas.ummi.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.szainabbas.ummi.ui.theme.UmmiIcons

/**
 * The 5-tab bottom nav from the handoff. Reminders is pushed on top, from More.
 * Visits is off the bar for now (Aimen's feedback, 1 Oct 2026); its screen and
 * route stay, so turning [inBottomBar] back on brings it back.
 */
enum class UmmiDestination(val route: String, val label: String, val icon: ImageVector, val inBottomBar: Boolean = true) {
    Today("today", "Today", UmmiIcons.today),
    Journey("journey", "Journey", UmmiIcons.journey),
    Duas("duas", "Duas", UmmiIcons.duas),
    Visits("visits", "Visits", UmmiIcons.visits, inBottomBar = false),
    More("more", "More", UmmiIcons.more),
    ;

    companion object {
        val bottomBar: List<UmmiDestination> get() = entries.filter { it.inBottomBar }
    }
}

object Routes {
    const val REMINDERS = "reminders"
}
