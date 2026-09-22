package com.strategy.blackjacktrainer.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Global appearance switch so every existing screen follows the same palette. */
object AppColorMode {
    var blackAndWhite by mutableStateOf(false)
}

private fun themed(color: Color): Color {
    if (!AppColorMode.blackAndWhite) return color
    val luminance = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f).coerceIn(0f, 1f)
    return Color(luminance, luminance, luminance, color.alpha)
}

// Table / background
private val BaseTableBg = Color(0xFF0F1512)
private val BaseSurfaceDark = Color(0xFF19211D)
private val BaseSurfaceElevated = Color(0xFF212B25)
private val BaseOutlineSoft = Color(0xFF33403A)
val TableBg: Color get() = themed(BaseTableBg)
val SurfaceDark: Color get() = themed(BaseSurfaceDark)
val SurfaceElevated: Color get() = themed(BaseSurfaceElevated)
val OutlineSoft: Color get() = themed(BaseOutlineSoft)

// Accent
private val BaseEmerald = Color(0xFF1CA672)
private val BaseEmeraldBright = Color(0xFF2ED996)
private val BaseGold = Color(0xFFE3B94A)
private val BaseGoldDim = Color(0xFF8A7238)
val Emerald: Color get() = themed(BaseEmerald)
val EmeraldBright: Color get() = themed(BaseEmeraldBright)
val Gold: Color get() = themed(BaseGold)
val GoldDim: Color get() = themed(BaseGoldDim)

// Feedback
private val BaseCorrectGreen = Color(0xFF34D399)
private val BaseMistakeRed = Color(0xFFF16565)
val CorrectGreen: Color get() = themed(BaseCorrectGreen)
val MistakeRed: Color get() = themed(BaseMistakeRed)

// Card face
private val BaseCardFace = Color(0xFFF7F3EA)
private val BaseCardBack = Color(0xFF14201A)
private val BaseCardInk = Color(0xFF1B1F1D)
private val BaseCardRed = Color(0xFFC23B3B)
val CardFace: Color get() = themed(BaseCardFace)
val CardBack: Color get() = themed(BaseCardBack)
val CardInk: Color get() = themed(BaseCardInk)
val CardRed: Color get() = themed(BaseCardRed)

// Text
private val BaseTextPrimary = Color(0xFFEFF3F0)
private val BaseTextSecondary = Color(0xFF9AAAA1)
private val BaseOnPrimaryText = Color(0xFF04120C)
val TextPrimary: Color get() = themed(BaseTextPrimary)
val TextSecondary: Color get() = themed(BaseTextSecondary)
val OnPrimaryText: Color get() = themed(BaseOnPrimaryText)

// Chart-only neutral cell
val NeutralCell: Color get() = themed(Color(0xFF2A342E))
