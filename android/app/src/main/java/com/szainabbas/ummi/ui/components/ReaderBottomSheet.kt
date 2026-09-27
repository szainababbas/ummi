package com.szainabbas.ummi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi

/**
 * The global "Read" sheet: a Sūrah in full, a single āyah, or a dua text,
 * resolved from [ReaderKey] against the bundled content. Arabic never
 * clamps — line-height 2x+ and wrapping, per the handoff's one hard rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomSheet(
    readerKey: ReaderKey,
    data: UmmiData,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (readerKey) {
            is ReaderKey.Surah -> {
                val surah = data.surahs[readerKey.no.toString()]
                if (surah != null) {
                    ReaderHeader(title = surah.name, subtitle = "${surah.meaning} · ${surah.v.size} āyāt", onClose = onDismiss)
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        if (surah.bism) {
                            item {
                                Text(
                                    text = data.bismillah.ar,
                                    fontFamily = Amiri,
                                    fontSize = 26.sp,
                                    lineHeight = 52.sp,
                                    textAlign = TextAlign.Center,
                                    color = Ummi.colors.accentText,
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                )
                                HorizontalDivider(color = Ummi.colors.line)
                            }
                        }
                        items(surah.v) { verse -> VerseRow(verse.n, verse.ar, verse.tl, verse.en) }
                        item { ReaderSource("Translation: ʿAlī Qulī Qaraʾī") }
                    }
                }
            }

            is ReaderKey.Ayah -> {
                val ayah = data.ayahs[readerKey.ref]
                if (ayah != null) {
                    ReaderHeader(title = ayah.ref, subtitle = null, onClose = onDismiss)
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { VerseRow(null, ayah.ar, ayah.tl, ayah.en) }
                        item { ReaderSource("Translation: ʿAlī Qulī Qaraʾī") }
                    }
                }
            }

            is ReaderKey.Dua -> {
                val text = data.duaTexts[readerKey.id]
                if (text != null) {
                    ReaderHeader(title = text.lbl, subtitle = null, onClose = onDismiss)
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { VerseRow(null, text.ar, text.tl, text.en) }
                        item { ReaderSource(text.sr) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderHeader(title: String, subtitle: String?, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ummi.colors.ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 13.sp, color = Ummi.colors.ink2)
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).clip(CircleShape),
        ) {
            Icon(UmmiIcons.close, contentDescription = "Close")
        }
    }
}

@Composable
private fun VerseRow(number: Int?, arabic: String, translit: String, english: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        if (number != null) {
            Text(
                text = "$number",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ummi.colors.accentText,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = arabic,
            fontFamily = Amiri,
            fontSize = 27.sp,
            lineHeight = 57.sp,
            textAlign = TextAlign.Right,
            color = Ummi.colors.ink,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(text = translit, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Ummi.colors.primary)
        Spacer(Modifier.height(6.dp))
        Text(text = english, fontSize = 15.sp, lineHeight = 24.sp, color = Ummi.colors.ink)
    }
    HorizontalDivider(color = Ummi.colors.line)
}

@Composable
private fun ReaderSource(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        fontStyle = FontStyle.Italic,
        fontSize = 12.sp,
        color = Ummi.colors.accentText,
        modifier = Modifier.fillMaxWidth().padding(20.dp),
    )
}
