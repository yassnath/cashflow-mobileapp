package com.solvix.tabungan

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeName(val label: String) {
  StandardLight("Standard Light"),
  StandardDark("Standard Dark"),
  CartoonFood("Kartun Makanan"),
  CartoonSpace("Kartun Luar Angkasa"),
  CartoonMonster("Kartun Monster"),
  CartoonHero("Kartun Superhero"),
  CartoonSea("Kartun Laut"),
  CartoonPlant("Kartun Tanaman"),
  CartoonPinky("Kartun Pinky"),
  CartoonColorful("Kartun Colorful"),
}

@Immutable
data class AppColors(
  val bg: Color,
  val bg2: Color,
  val text: Color,
  val muted: Color,
  val placeholder: Color,
  val card: Color,
  val cardBorder: Color,
  val fieldBorder: Color,
  val accent: Color,
  val accent2: Color,
  val danger: Color,
)

private val StandardLightPalette = AppColors(
  bg = Color(0xFFF4F7FF),
  bg2 = Color(0xFFEAF2FF),
  text = Color(0xFF1C2141),
  muted = Color(0xFF6F7AA5),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFFFFFFF),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFFFF9F1C),
  accent2 = Color(0xFF2EC4B6),
  danger = Color(0xFFFF4D6D),
)

private val StandardDarkPalette = AppColors(
  bg = Color(0xFF0F1220),
  bg2 = Color(0xFF1B2034),
  text = Color(0xFFFFFFFF),
  muted = Color(0xCCFFFFFF),
  placeholder = Color(0x99FFFFFF),
  card = Color(0xFF14182C),
  cardBorder = Color(0x14FFFFFF),
  fieldBorder = Color(0x59FFFFFF),
  accent = Color(0xFF7F5AF0),
  accent2 = Color(0xFF2CB67D),
  danger = Color(0xFFFF5470),
)

private val CartoonFoodPalette = AppColors(
  bg = Color(0xFFFFF4E6),
  bg2 = Color(0xFFFFE9CC),
  text = Color(0xFF4A2F1C),
  muted = Color(0xFF9C6A42),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFFFFAF3),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFFFF7A00),
  accent2 = Color(0xFFFFB703),
  danger = Color(0xFFFF4D6D),
)

private val CartoonSpacePalette = AppColors(
  bg = Color(0xFF101236),
  bg2 = Color(0xFF1A1F4D),
  text = Color(0xFFFFFFFF),
  muted = Color(0xCCFFFFFF),
  placeholder = Color(0x99FFFFFF),
  card = Color(0xFF171C44),
  cardBorder = Color(0x1AFFFFFF),
  fieldBorder = Color(0x4DFFFFFF),
  accent = Color(0xFF9D4EDD),
  accent2 = Color(0xFF5FA8D3),
  danger = Color(0xFFFF5470),
)

private val CartoonMonsterPalette = AppColors(
  bg = Color(0xFFF0FFF4),
  bg2 = Color(0xFFD6FFE5),
  text = Color(0xFF134D35),
  muted = Color(0xFF4C8B6A),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFF8FFFB),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFF2EC4B6),
  accent2 = Color(0xFF8AC926),
  danger = Color(0xFFFF4D6D),
)

private val CartoonHeroPalette = AppColors(
  bg = Color(0xFFEAF2FF),
  bg2 = Color(0xFFD6E6FF),
  text = Color(0xFF14214B),
  muted = Color(0xFF4F6DB3),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFF6F9FF),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFFFF595E),
  accent2 = Color(0xFF1982C4),
  danger = Color(0xFFFF4D6D),
)

private val CartoonSeaPalette = AppColors(
  bg = Color(0xFFE8F8FF),
  bg2 = Color(0xFFD4EFFF),
  text = Color(0xFF173A4F),
  muted = Color(0xFF4C7A96),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFF6FBFF),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFF2EC4B6),
  accent2 = Color(0xFF4DABF7),
  danger = Color(0xFFFF4D6D),
)

private val CartoonPlantPalette = AppColors(
  bg = Color(0xFFF5FFF0),
  bg2 = Color(0xFFE0FFD4),
  text = Color(0xFF234D20),
  muted = Color(0xFF5C8A51),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFFBFFF8),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFF80B918),
  accent2 = Color(0xFF2D6A4F),
  danger = Color(0xFFFF4D6D),
)

private val CartoonPinkyPalette = AppColors(
  bg = Color(0xFFFFF0F6),
  bg2 = Color(0xFFFFD6E8),
  text = Color(0xFF5B1B3C),
  muted = Color(0xFFB85A83),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFFFF7FB),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFFFF4D6D),
  accent2 = Color(0xFFFF85A1),
  danger = Color(0xFFFF4D6D),
)

private val CartoonColorfulPalette = AppColors(
  bg = Color(0xFFF0F7FF),
  bg2 = Color(0xFFE0ECFF),
  text = Color(0xFF2A2F5B),
  muted = Color(0xFF6F78B8),
  placeholder = Color(0xFF9AA3B4),
  card = Color(0xFFFEFCFF),
  cardBorder = Color(0x1F111827),
  fieldBorder = Color(0x1F111827),
  accent = Color(0xFFFF6B6B),
  accent2 = Color(0xFF4DABF7),
  danger = Color(0xFFFF4D6D),
)

private val ThemePalettes = mapOf(
  ThemeName.StandardLight to StandardLightPalette,
  ThemeName.StandardDark to StandardDarkPalette,
  ThemeName.CartoonFood to CartoonFoodPalette,
  ThemeName.CartoonSpace to CartoonSpacePalette,
  ThemeName.CartoonMonster to CartoonMonsterPalette,
  ThemeName.CartoonHero to CartoonHeroPalette,
  ThemeName.CartoonSea to CartoonSeaPalette,
  ThemeName.CartoonPlant to CartoonPlantPalette,
  ThemeName.CartoonPinky to CartoonPinkyPalette,
  ThemeName.CartoonColorful to CartoonColorfulPalette,
)

val LocalAppColors = staticCompositionLocalOf { StandardLightPalette }
val LocalThemeName = staticCompositionLocalOf { ThemeName.StandardLight }

private val AppFontFamily = FontFamily(
  Font(R.font.fredoka_regular, FontWeight.Normal),
  Font(R.font.fredoka_medium, FontWeight.Medium),
  Font(R.font.fredoka_semibold, FontWeight.SemiBold),
  Font(R.font.fredoka_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
  bodyLarge = TextStyle(fontFamily = AppFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal),
  bodyMedium = TextStyle(fontFamily = AppFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal),
  bodySmall = TextStyle(fontFamily = AppFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal),
  titleLarge = TextStyle(fontFamily = AppFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
  titleMedium = TextStyle(fontFamily = AppFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold),
  titleSmall = TextStyle(fontFamily = AppFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun TabunganTheme(theme: ThemeName, content: @Composable () -> Unit) {
  val palette = ThemePalettes[theme] ?: StandardLightPalette
  val colorScheme = colorSchemeFor(theme, palette)
  CompositionLocalProvider(
    LocalAppColors provides palette,
    LocalThemeName provides theme,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      content = content,
    )
  }
}

fun isDarkTheme(theme: ThemeName): Boolean {
  return theme == ThemeName.StandardDark || theme == ThemeName.CartoonSpace
}

private fun colorSchemeFor(theme: ThemeName, palette: AppColors): ColorScheme {
  val base = if (theme == ThemeName.StandardDark || theme == ThemeName.CartoonSpace) {
    darkColorScheme()
  } else {
    lightColorScheme()
  }
  return base.copy(
    primary = palette.accent,
    secondary = palette.accent2,
    error = palette.danger,
    background = palette.bg,
    surface = palette.card,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = palette.text,
    onSurface = palette.text,
    onError = Color.White,
  )
}
