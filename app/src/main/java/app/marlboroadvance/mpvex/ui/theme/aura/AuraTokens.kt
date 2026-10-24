package app.marlboroadvance.mpvex.ui.theme.aura

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Aura Design System Tokens
 * An independent, modern, high-contrast, premium aesthetic built for media consumption.
 */
object AuraTokens {
    // Foundational Palette - Pure Obsidian & Carbon Grays
    val VoidBlack = Color(0xFF060709)
    val ObsidianDark = Color(0xFF0C0E12)
    val ObsidianSurface = Color(0xFF0C0E12)
    val CarbonElevated = Color(0xFF14171F)
    val CarbonCard = Color(0xFF14171F)
    val SlateSurface = Color(0xFF1B202A)
    val BorderSubtle = Color(0xFF282E3D)
    val CarbonBorder = Color(0xFF282E3D)
    val BorderHighlight = Color(0xFF3B445A)

    val PureWhite = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFF2F5FA)
    val TextPrimary = Color(0xFFF0F4F8)
    val TextSecondary = Color(0xFF9BA3B5)
    val TextTertiary = Color(0xFF6B7285)

    // Curated Aura Brand Accents
    val AuraCyan = Color(0xFF00E5FF)
    val AuraCyanGlow = Color(0x3300E5FF)
    val AuraCyanContainer = Color(0xFF003842)

    val AuraViolet = Color(0xFFBB86FC)
    val AuraVioletGlow = Color(0x33BB86FC)
    val AuraVioletContainer = Color(0xFF3C1860)

    val AuraEmerald = Color(0xFF00E676)
    val AuraEmeraldGlow = Color(0x3300E676)
    val AuraEmeraldContainer = Color(0xFF003D20)

    val AuraSunset = Color(0xFFFF9100)
    val AuraSunsetGlow = Color(0x33FF9100)
    val AuraSunsetContainer = Color(0xFF4E2C00)

    val AuraRose = Color(0xFFFF4081)
    val AuraRoseGlow = Color(0x33FF4081)
    val AuraRoseContainer = Color(0xFF4E0020)

    // Standard Functional Colors
    val Success = Color(0xFF00E676)
    val Warning = Color(0xFFFFD600)
    val Error = Color(0xFFFF5252)
    val ErrorContainer = Color(0xFF4C0005)

    // Shapes
    val ShapeCard = RoundedCornerShape(16.dp)
    val ShapeCardLarge = RoundedCornerShape(20.dp)
    val ShapeDialog = RoundedCornerShape(24.dp)
    val ShapeSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val ShapeThumbnail = RoundedCornerShape(12.dp)
    val ShapeChip = RoundedCornerShape(8.dp)
    val ShapePill = CircleShape

    // Glass & Ambient Brushes
    val GlassOverlayDark = Color(0xCC0E1117)
    val GlassOverlayLight = Color(0xE6FFFFFF)
    val GlassBorderDark = Color(0x33FFFFFF)
    val GlassBorderLight = Color(0x22000000)

    fun playerGradientBrush(topDark: Boolean = true): Brush {
        return Brush.verticalGradient(
            colors = if (topDark) {
                listOf(
                    Color.Black.copy(alpha = 0.85f),
                    Color.Black.copy(alpha = 0.35f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.92f)
                )
            } else {
                listOf(
                    Color.Black.copy(alpha = 0.90f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.90f)
                )
            }
        )
    }

    fun subtleCardGradient(isDark: Boolean, accent: Color): Brush {
        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    CarbonElevated,
                    CarbonElevated.copy(alpha = 0.92f),
                    SlateSurface.copy(alpha = 0.7f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    PureWhite,
                    OffWhite
                )
            )
        }
    }
}

/**
 * Typography definition for Aura Player
 */
val AuraTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp
    )
)
