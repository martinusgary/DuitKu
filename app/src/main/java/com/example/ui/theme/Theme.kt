package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryPurple,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryWarm,
    onTertiary = OnTertiaryWhite,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = AppBackground,
    onBackground = AppOnBackground,
    surface = AppBackground,
    onSurface = AppOnBackground,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = OnSurfaceVariantColor,
    error = ErrorColor
  )

private val MintLightColorScheme = lightColorScheme(
    primary = Color(0xFF00B1A9), // Gopay Indigo / Teal Mint style
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1F7F4),
    onPrimaryContainer = Color(0xFF003734),
    secondary = Color(0xFF059669), // Fresh Emerald
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF4FBF9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2F0ED),
    onSurfaceVariant = Color(0xFF334155),
    error = Color(0xFFB3261E)
)

private val MintDarkColorScheme = darkColorScheme(
    primary = Color(0xFF14DFD4),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFFD1F7F4),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    background = Color(0xFF081211),
    onBackground = Color(0xFFE2F0ED),
    surface = Color(0xFF0D1B19),
    onSurface = Color(0xFFE2F0ED),
    surfaceVariant = Color(0xFF1B2E2B),
    onSurfaceVariant = Color(0xFFD1F7F4)
)

private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF0564CA), // Jago/BCA/Royal trust blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E9FF),
    onPrimaryContainer = Color(0xFF001D45),
    secondary = Color(0xFF0EA5E9), // Sky blue
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    background = Color(0xFFF3F7FC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0ECFC),
    onSurfaceVariant = Color(0xFF334155),
    error = Color(0xFFB3261E)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0369A1),
    primaryContainer = Color(0xFF0A39A2),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0B132B),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF141D35),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF5E14), // Jenius Orange Glow
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEADF),
    onPrimaryContainer = Color(0xFF521500),
    secondary = Color(0xFFEAB308), // Golden Yellow
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFFFEF9C3),
    onSecondaryContainer = Color(0xFF713F12),
    background = Color(0xFFFFF9F6),
    onBackground = Color(0xFF1E1E24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFFFBECE5),
    onSurfaceVariant = Color(0xFF4A3E39),
    error = Color(0xFFB3261E)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8E53),
    onPrimary = Color(0xFF4E1A00),
    primaryContainer = Color(0xFF802D02),
    onPrimaryContainer = Color(0xFFFFEADF),
    secondary = Color(0xFFFACC15),
    onSecondary = Color(0xFF3F2B00),
    background = Color(0xFF1A120B),
    onBackground = Color(0xFFFAEBD7),
    surface = Color(0xFF231B14),
    onSurface = Color(0xFFFAEBD7),
    surfaceVariant = Color(0xFF3B281B),
    onSurfaceVariant = Color(0xFFFFEADF)
)

private val SakuraLightColorScheme = lightColorScheme(
    primary = Color(0xFFD05090), // Sakura Pink
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFECF6),
    onPrimaryContainer = Color(0xFF46002A),
    secondary = Color(0xFF9C27B0), // Purple elegance
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF31004A),
    background = Color(0xFFFFF6FB),
    onBackground = Color(0xFF2C1921),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C1921),
    surfaceVariant = Color(0xFFF5E4EE),
    onSurfaceVariant = Color(0xFF53434B),
    error = Color(0xFFB3261E)
)

private val SakuraDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF560027),
    primaryContainer = Color(0xFF880E4F),
    onPrimaryContainer = Color(0xFFFFECF6),
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF4A0072),
    background = Color(0xFF2D0B1E),
    onBackground = Color(0xFFFAECF0),
    surface = Color(0xFF341022),
    onSurface = Color(0xFFFAECF0),
    surfaceVariant = Color(0xFF531E3B),
    onSurfaceVariant = Color(0xFFFFECF6)
)

@Composable
fun MyApplicationTheme(
  themeName: String = "CLASSIC",
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val colorScheme =
    when {
      themeName == "DYNAMIC" && supportsDynamic -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> {
        when (themeName) {
          "MINT" -> if (darkTheme) MintDarkColorScheme else MintLightColorScheme
          "OCEAN" -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
          "SUNSET" -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
          "SAKURA" -> if (darkTheme) SakuraDarkColorScheme else SakuraLightColorScheme
          "DYNAMIC" -> if (darkTheme) DarkColorScheme else LightColorScheme
          else -> if (darkTheme) DarkColorScheme else LightColorScheme
        }
      }
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
