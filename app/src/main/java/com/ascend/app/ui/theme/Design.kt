package com.ascend.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The design system: every measurement in the app comes from here.
 *
 * Before this existed, sizes were chosen per call site — 11.sp here, 13.sp
 * there, 9.dp of padding in one card and 10.dp in the next. Individually
 * invisible, collectively the reason a screen feels assembled rather than
 * designed. The fix is not better guesses, it is removing the guess: a small
 * scale, used everywhere, so things line up because they are drawn from the
 * same ruler rather than because someone eyeballed them.
 *
 * The scales are deliberately short. A long scale is the same problem wearing
 * a nicer hat — if there are eleven text sizes, two of them are a coin flip.
 */
object Space {
    /** Hairline gaps inside a single control. */
    val xxs = 2.dp
    val xs = 4.dp
    /** Between tightly-related lines, e.g. a value and its caption. */
    val sm = 8.dp
    /** The default gap between elements inside a card. */
    val md = 12.dp
    /** Between cards in a list, and card inner padding. */
    val lg = 16.dp
    /** Screen side margins, and separation between sections. */
    val xl = 20.dp
    /** Above a new section heading — the app's largest breathing space. */
    val xxl = 28.dp
}

object Radius {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(18.dp)
    val pill = RoundedCornerShape(50)
}

/**
 * Type scale.
 *
 * Six steps, each with a fixed weight and line height, because the pairing is
 * what makes text look considered — a 22sp heading at Normal weight and a 22sp
 * heading at ExtraBold are different typefaces as far as the eye is concerned.
 */
object Type {
    /** Screen titles. */
    val displaySize = 26.sp
    val displayWeight = FontWeight.ExtraBold
    val displayTracking = 0.5.sp

    /** Big numbers: XP, streak counts, stat levels. */
    val statSize = 24.sp
    val statWeight = FontWeight.ExtraBold

    /** Card titles and habit names. */
    val titleSize = 15.sp
    val titleWeight = FontWeight.SemiBold
    val titleLineHeight = 20.sp

    /** Body copy and explanations. */
    val bodySize = 13.sp
    val bodyWeight = FontWeight.Normal
    val bodyLineHeight = 19.sp

    /** Supporting detail under a title. */
    val captionSize = 11.sp
    val captionWeight = FontWeight.Normal
    val captionLineHeight = 15.sp

    /** Section eyebrows and pills — always uppercase, always tracked out. */
    val labelSize = 10.sp
    val labelWeight = FontWeight.Bold
    val labelTracking = 1.4.sp
}

/**
 * How strongly a surface separates itself from the background.
 *
 * Named by intent rather than by depth number, so a screen asks for "the
 * card that carries the main answer" instead of picking an elevation and
 * hoping it reads as more important than its neighbour.
 */
enum class SurfaceLevel {
    /** Sits quietly in a list. Most cards. */
    Resting,

    /** The one thing on the screen worth reading first. */
    Raised,

    /** Demands attention: an error, a level-up, a System message. */
    Highlighted,
}

/** Shared alignment for centred empty states. */
val CenteredText = TextAlign.Center
