package com.szainabbas.ummi.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.szainabbas.ummi.ui.theme.UmmiIcons

/** The 5-tab bottom nav from the handoff. Visits and Reminders are placeholder
 * screens for now (full features land in later PRs) but the destinations
 * exist so the shell is complete. */
enum class UmmiDestination(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Today", UmmiIcons.today),
    Journey("journey", "Journey", UmmiIcons.journey),
    Duas("duas", "Duas", UmmiIcons.duas),
    Visits("visits", "Visits", UmmiIcons.visits),
    More("more", "More", UmmiIcons.more),
}

object Routes {
    const val REMINDERS = "reminders"
}
