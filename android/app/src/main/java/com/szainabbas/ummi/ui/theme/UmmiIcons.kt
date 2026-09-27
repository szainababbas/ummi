package com.szainabbas.ummi.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationAdd
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
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
 * (e.g. "stethoscope"), the nearest existing icon is used instead and noted here.
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
    val timePrayer: ImageVector = Icons.Rounded.Mosque
    val bellAdd: ImageVector = Icons.Rounded.NotificationAdd
    val notifications: ImageVector = Icons.Rounded.Notifications
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

    // Visits
    val add: ImageVector = Icons.Rounded.Add
    val location: ImageVector = Icons.Rounded.LocationOn
    val tip: ImageVector = Icons.Rounded.Lightbulb
    val typeMidwife: ImageVector = Icons.Rounded.LocalHospital // stand-in for "stethoscope"
    val typeScan: ImageVector = Icons.Rounded.MonitorHeart
    val typeBlood: ImageVector = Icons.Rounded.Bloodtype
    val typeGp: ImageVector = Icons.Rounded.MedicalServices
    val typeConsultant: ImageVector = Icons.Rounded.Person
    val typeOther: ImageVector = Icons.Rounded.MoreHoriz

    // Reminders and onboarding
    val back: ImageVector = Icons.Rounded.ArrowBack
    val allowed: ImageVector = Icons.Rounded.CheckCircle
    val water: ImageVector = Icons.Rounded.WaterDrop
    val lock: ImageVector = Icons.Rounded.Lock
    val minus: ImageVector = Icons.Rounded.Remove

    // More screen
    val favourite: ImageVector = Icons.Rounded.Favorite
    val favouriteOff: ImageVector = Icons.Rounded.FavoriteBorder
    val edit: ImageVector = Icons.Rounded.Edit
    val download: ImageVector = Icons.Rounded.FileDownload
    val restore: ImageVector = Icons.Rounded.Restore
    val readableRecord: ImageVector = Icons.Rounded.Description
    val resetApp: ImageVector = Icons.Rounded.DeleteForever
}
