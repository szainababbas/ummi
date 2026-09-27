package com.szainabbas.ummi.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PregnantWoman
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every icon glyph the app uses, in one place. The handoff specs "Material
 * Symbols Rounded" (Google's newer variable-font icon set); this app instead
 * uses `material-icons-extended`, an older fixed catalogue that ships as a
 * normal Gradle dependency and needs no font-variation plumbing. Most names
 * match directly. Where the handoff's exact glyph isn't in that catalogue
 * (e.g. "mosque", "stethoscope"), the nearest existing icon is used instead and noted here.
 * Swapping in the real Material Symbols font is a possible follow-up, not
 * a blocker.
 */
object UmmiIcons {
    // Bottom navigation
    val today: ImageVector = Icons.Rounded.WbSunny
    val journey: ImageVector = Icons.Rounded.PregnantWoman
    val duas: ImageVector = Icons.Rounded.MenuBook
    val visits: ImageVector = Icons.Rounded.Event
    val more: ImageVector = Icons.Rounded.MoreHoriz

    // Today screen
    val bell: ImageVector = Icons.Rounded.NotificationsActive
    val bellOff: ImageVector = Icons.Rounded.NotificationsOff
    val check: ImageVector = Icons.Rounded.Check
    val timeMorning: ImageVector = Icons.Rounded.WbTwilight
    val timePrayer: ImageVector = Icons.Rounded.AccessTime // stand-in for "mosque"
    val timeEvening: ImageVector = Icons.Rounded.NightsStay
    val timeAnytime: ImageVector = Icons.Rounded.Schedule

    // Journey screen
    val prevWeek: ImageVector = Icons.Rounded.KeyboardArrowLeft
    val nextWeek: ImageVector = Icons.Rounded.KeyboardArrowRight
    val milestone: ImageVector = Icons.Rounded.Flag
    val catRecitation: ImageVector = Icons.Rounded.AutoStories
    val catSurah: ImageVector = Icons.Rounded.MenuBook
    val catFood: ImageVector = Icons.Rounded.Restaurant // stand-in for "nutrition"
    val catWellness: ImageVector = Icons.Rounded.Spa
    val avoid: ImageVector = Icons.Rounded.Block

    // Duas / reader sheet
    val chevronDown: ImageVector = Icons.Rounded.ExpandMore
    val close: ImageVector = Icons.Rounded.Close
    val schedule: ImageVector = Icons.Rounded.Schedule

    // Visits screen. There's no stethoscope in this icon set, so midwife
    // visits use the same figure as the Journey tab.
    val add: ImageVector = Icons.Rounded.Add
    val event: ImageVector = Icons.Rounded.Event
    val delete: ImageVector = Icons.Rounded.Delete
    val reminderSet: ImageVector = Icons.Rounded.Notifications
    val typeMidwife: ImageVector = Icons.Rounded.PregnantWoman
    val typeScan: ImageVector = Icons.Rounded.MonitorHeart
    val typeBlood: ImageVector = Icons.Rounded.Bloodtype
    val typeGp: ImageVector = Icons.Rounded.MedicalServices
    val typeConsultant: ImageVector = Icons.Rounded.Person
    val typeOther: ImageVector = Icons.Rounded.MoreHoriz

    // More screen
    val edit: ImageVector = Icons.Rounded.Edit
    val download: ImageVector = Icons.Rounded.FileDownload
    val restore: ImageVector = Icons.Rounded.Restore
    val readableRecord: ImageVector = Icons.Rounded.Description
    val resetApp: ImageVector = Icons.Rounded.DeleteForever

    // Reminders, onboarding and the names wishlist
    val back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val checkCircle: ImageVector = Icons.Rounded.CheckCircle
    val water: ImageVector = Icons.Rounded.WaterDrop
    val bellAdd: ImageVector = Icons.Rounded.NotificationsNone // stand-in for "notification_add"
    val lock: ImageVector = Icons.Rounded.Lock
    val remove: ImageVector = Icons.Rounded.Remove
    val favourite: ImageVector = Icons.Rounded.Favorite
    val notFavourite: ImageVector = Icons.Rounded.FavoriteBorder
}
