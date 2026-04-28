package com.example.scheduleiseu.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.scheduleiseu.R

private val BaseLetterSpacing = (-0.04).em

val AppFontFamily = FontFamily(
    Font(R.font.certa_medium, FontWeight.Normal),
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 20.sp,
        lineHeight = 22.sp,
        letterSpacing = BaseLetterSpacing,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        letterSpacing = BaseLetterSpacing,
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        letterSpacing = BaseLetterSpacing,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        letterSpacing = BaseLetterSpacing,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = BaseLetterSpacing,
    ),
)
