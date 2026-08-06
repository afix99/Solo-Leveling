package com.ascend.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat

/** UI-layer color mapping for domain enums — keeps [Stat]/[Rank] themselves
 * free of any Compose dependency. */
fun Stat.color(): Color = when (this) {
    Stat.STR -> AscendColors.StatStr
    Stat.VIT -> AscendColors.StatVit
    Stat.INT -> AscendColors.StatInt
    Stat.PER -> AscendColors.StatPer
    Stat.AGI -> AscendColors.StatAgi
}

fun Rank.color(): Color = when (this) {
    Rank.E -> AscendColors.RankE
    Rank.D -> AscendColors.RankD
    Rank.C -> AscendColors.RankC
    Rank.B -> AscendColors.RankB
    Rank.A -> AscendColors.RankA
    Rank.S -> AscendColors.RankS
}
