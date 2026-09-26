package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi

/** Stand-in for Visits and Reminders until their own PRs land the real screens. */
@Composable
fun ComingSoonScreen(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, fontSize = 14.sp, color = Ummi.colors.ink2, textAlign = TextAlign.Center)
    }
}
