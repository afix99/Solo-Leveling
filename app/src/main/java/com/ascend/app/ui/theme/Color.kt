package com.ascend.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ASCEND "System Window" palette.
 *
 * One dark theme only (v1). Deliberately restrained: a single blue accent for
 * everyday progress, violet reserved for rank-up moments, amber for penalties/
 * warnings. Everything else is near-black/gray to keep focus on the numbers.
 */
object AscendColors {
    val Background = Color(0xFF0A0B0F)
    val Surface = Color(0xFF14161D)
    val SurfaceElevated = Color(0xFF1C1F29)
    val SurfaceElevated2 = Color(0xFF262A36)
    val Divider = Color(0xFF23262F)

    val AccentBlue = Color(0xFF4C8DFF)
    val AccentBlueDim = Color(0xFF2E5AA8)
    val AccentViolet = Color(0xFF8B5CF6)
    val AccentVioletDim = Color(0xFF5A3AA6)
    val Amber = Color(0xFFF5A623)
    val AmberDim = Color(0xFF8A5E14)
    val Success = Color(0xFF34D399)
    val Danger = Color(0xFFEF4444)

    val TextPrimary = Color(0xFFF2F3F5)
    val TextSecondary = Color(0xFF9AA0AC)
    val TextTertiary = Color(0xFF5C616D)

    // Stat identity colors — used consistently for stat badges/bars/charts.
    val StatStr = Color(0xFFEF6A5B) // physical / body
    val StatVit = Color(0xFF34D399) // health / maintenance
    val StatInt = Color(0xFF4C8DFF) // learning / deep work
    val StatPer = Color(0xFF8B5CF6) // mindfulness / awareness
    val StatAgi = Color(0xFFF5C74A) // momentum / consistency

    // Hunter rank colors, E -> S.
    val RankE = Color(0xFF9AA0AC)
    val RankD = Color(0xFF5EC2E8)
    val RankC = Color(0xFF34D399)
    val RankB = Color(0xFF4C8DFF)
    val RankA = Color(0xFF8B5CF6)
    val RankS = Color(0xFFF5C74A)
}
