package com.szainabbas.ummi.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.model.DuaEntry
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi

private val DUA_TAB_ORDER = listOf("Pregnancy", "Easy delivery", "After birth", "Daily dhikr")

@Composable
fun DuasScreen(data: UmmiData, onOpenReader: (ReaderKey) -> Unit) {
    val tabs = DUA_TAB_ORDER.filter { data.duas.containsKey(it) } + "Qurʾān"
    var selectedTab by remember { mutableStateOf(tabs.first()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Duas & Qurʾān", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, modifier = Modifier.padding(20.dp, 12.dp), color = Ummi.colors.ink)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Text(
                    tab,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Ummi.colors.onPrimary else Ummi.colors.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Ummi.colors.primary else Ummi.colors.surface)
                        .border(if (isSelected) 0.dp else 1.dp, Ummi.colors.line, RoundedCornerShape(18.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            if (selectedTab == "Qurʾān") {
                QuranTab(data, onOpenReader)
            } else {
                val duaList = data.duas[selectedTab].orEmpty()
                var openIndex by remember(selectedTab) { mutableStateOf(0) }
                duaList.forEachIndexed { index, dua ->
                    DuaAccordionCard(dua = dua, expanded = index == openIndex, onToggle = { openIndex = if (openIndex == index) -1 else index })
                }
                Text(
                    "Sources: From Marriage to Parenthood (KSIMC), al-islam.org, and traditions of the Ahlulbayt (as).",
                    fontSize = 13.sp,
                    color = Ummi.colors.ink2,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DuaAccordionCard(dua: DuaEntry, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dua.title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ummi.colors.ink)
                if (dua.whenText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(UmmiIcons.schedule, contentDescription = null, tint = Ummi.colors.ink2, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(dua.whenText, fontSize = 13.sp, color = Ummi.colors.ink2)
                    }
                }
            }
            Icon(UmmiIcons.chevronDown, contentDescription = null, tint = Ummi.colors.ink2, modifier = Modifier.rotate(rotation))
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            if (!dua.arabic.isNullOrBlank()) {
                Text(
                    dua.arabic,
                    fontFamily = Amiri,
                    fontSize = 26.sp,
                    lineHeight = 52.sp,
                    textAlign = TextAlign.Center,
                    color = Ummi.colors.ink,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (dua.translit.isNotBlank()) {
                Text(dua.translit, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = Ummi.colors.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (dua.body.isNotBlank()) {
                Text(dua.body, fontSize = 15.sp, lineHeight = 23.sp, color = Ummi.colors.ink)
            }
            if (dua.src.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(dua.src, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Ummi.colors.accentText)
            }
        }
    }
}

@Composable
private fun QuranTab(data: UmmiData, onOpenReader: (ReaderKey) -> Unit) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
                .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("Short sūrahs in full", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ummi.colors.ink)
            Spacer(Modifier.height(8.dp))
            data.surahs.values.sortedBy { it.no }.forEach { surah ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReader(ReaderKey.forSurah(surah.no)) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Ummi.colors.surface2), contentAlignment = Alignment.Center) {
                        Text("${surah.no}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(surah.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ummi.colors.ink)
                        Text("${surah.meaning} · ${surah.v.size} āyāt", fontSize = 12.sp, color = Ummi.colors.ink2)
                    }
                    Text(surah.ar, fontFamily = Amiri, fontSize = 20.sp, color = Ummi.colors.accentText)
                }
            }
        }

        Text("Āyāt the guide asks for", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText, modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 4.dp))
        FlowChips(refs = data.ayahs.keys.sorted(), onClick = { onOpenReader(ReaderKey.forAyah(it)) })
    }
}

@Composable
private fun FlowChips(refs: List<String>, onClick: (String) -> Unit) {
    // Fixed-size rows instead of Compose's FlowRow, to avoid depending on
    // its exact API shape in this BOM version without being able to compile
    // and check it here.
    val chunkSize = 3
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        refs.chunked(chunkSize).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { ref ->
                    Text(
                        ref,
                        fontSize = 13.sp,
                        color = Ummi.colors.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, Ummi.colors.line, RoundedCornerShape(18.dp))
                            .clickable { onClick(ref) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
