package com.ascend.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ascend.app.R

/**
 * Inter (variable font, OFL-licensed) — a clean, geometric, Apple-Health-esque
 * face that reads well at both huge numeral sizes (level/XP) and small dense
 * labels. Single font file, weight pulled via variation settings.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val InterFamily = FontFamily(
    interWeight(400),
    interWeight(500),
    interWeight(600),
    interWeight(700),
    interWeight(800),
)

/** Extra-wide letter-spacing label style used for eyebrow/section headers. */
val LabelEyebrow = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight(600),
    fontSize = 12.sp,
    letterSpacing = 0.18.em,
)

/** Huge numeral style for level/XP/stat displays. */
val DisplayNumeral = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight(800),
    fontSize = 56.sp,
    letterSpacing = (-0.02).em,
)

val AscendTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(800),
        fontSize = 40.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(700),
        fontSize = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(700),
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(600),
        fontSize = 18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(600),
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(400),
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        letterSpacing = 0.02.em,
    ),
    labelMedium = LabelEyebrow,
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight(600),
        fontSize = 11.sp,
        letterSpacing = 0.1.em,
    ),
)
