package com.szainabbas.ummi.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every token from the design handoff's light/dark tables, kept as plain
 * Color values rather than folded into ColorScheme — several (accentText,
 * ac, nav, hero*) have no Material 3 slot, so [UmmiColors] carries them
 * alongside a real ColorScheme built from the ones that do.
 */
data class UmmiColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val primary: Color,
    val onPrimary: Color,
    val pc: Color,
    val onpc: Color,
    val accent: Color,
    val accentText: Color,
    val ac: Color,
    val heroA: Color,
    val heroB: Color,
    val heroInk: Color,
    val heroInk2: Color,
    val nav: Color,
    val danger: Color,
)

val LightUmmiColors = UmmiColors(
    bg = Color(0xFFF6F1E7),
    surface = Color(0xFFFFFDF8),
    surface2 = Color(0xFFEEE7D8),
    line = Color(0xFFE0D6C2),
    ink = Color(0xFF1E1C18),
    ink2 = Color(0xFF5A5446),
    primary = Color(0xFF2D4A3E),
    onPrimary = Color(0xFFFFFFFF),
    pc = Color(0xFFDCE8E0),
    onpc = Color(0xFF16332A),
    accent = Color(0xFFA07A45),
    accentText = Color(0xFF6E4F24),
    ac = Color(0xFFF2E6CC),
    heroA = Color(0xFF2D4A3E),
    heroB = Color(0xFF1C342B),
    heroInk = Color(0xFFFFFFFF),
    heroInk2 = Color(0xFFEBDDBB),
    nav = Color(0xFFEFE8DA),
    danger = Color(0xFFA5473A),
)

val DarkUmmiColors = UmmiColors(
    bg = Color(0xFF0F1411),
    surface = Color(0xFF171E1A),
    surface2 = Color(0xFF1F2823),
    line = Color(0xFF2A342E),
    ink = Color(0xFFECE6D9),
    ink2 = Color(0xFFAEB5AB),
    primary = Color(0xFFA3CBB5),
    onPrimary = Color(0xFF0D2A1E),
    pc = Color(0xFF26463A),
    onpc = Color(0xFFD2E8DB),
    accent = Color(0xFFD6B478),
    accentText = Color(0xFFE4C990),
    ac = Color(0xFF352C1C),
    heroA = Color(0xFF21443A),
    heroB = Color(0xFF162D25),
    heroInk = Color(0xFFF4EFE4),
    heroInk2 = Color(0xFFE4C990),
    nav = Color(0xFF151B18),
    danger = Color(0xFFE39A8A),
)
