package com.szainabbas.ummi.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.szainabbas.ummi.ui.theme.UmmiIcons

/** The 5-tab bottom nav from the handoff. Reminders is not a tab: it is pushed
 * from Today's bell or from More, and back returns to where she came from. */
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
