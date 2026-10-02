package io.github.keriol.butlerinterphone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val InterphoneColors = darkColorScheme(
    primary = Color(0xFFD0B477),
    onPrimary = Color(0xFF211A0E),
    primaryContainer = Color(0xFF4B3E23),
    onPrimaryContainer = Color(0xFFF6E8C5),
    secondary = Color(0xFF8BB7A0),
    onSecondary = Color(0xFF102019),
    secondaryContainer = Color(0xFF28463A),
    onSecondaryContainer = Color(0xFFD7EEE2),
    background = Color(0xFF0E1713),
    onBackground = Color(0xFFF2ECE2),
    surface = Color(0xFF14211B),
    onSurface = Color(0xFFF2ECE2),
    surfaceVariant = Color(0xFF203027),
    onSurfaceVariant = Color(0xFFD1D8D2),
    outline = Color(0xFF76877D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val InterphoneShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

private val InterphoneTypography = Typography(
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InterphoneTheme(
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = InterphoneColors,
        motionScheme = MotionScheme.expressive(),
        typography = InterphoneTypography,
        shapes = InterphoneShapes,
        content = content,
    )
}
