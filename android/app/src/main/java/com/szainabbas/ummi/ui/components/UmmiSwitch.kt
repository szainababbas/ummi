package com.szainabbas.ummi.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.szainabbas.ummi.ui.theme.Ummi

/** The handoff's switch: `primary` track and `onp` knob when on, `surface2` track with an `ink2` ring and knob when off. */
@Composable
fun UmmiSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Ummi.colors.primary,
            checkedThumbColor = Ummi.colors.onPrimary,
            checkedBorderColor = Ummi.colors.primary,
            uncheckedTrackColor = Ummi.colors.surface2,
            uncheckedThumbColor = Ummi.colors.ink2,
            uncheckedBorderColor = Ummi.colors.ink2,
        ),
    )
}
