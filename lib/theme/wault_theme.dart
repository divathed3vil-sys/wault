// File: lib/theme/wault_theme.dart
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'wault_colors.dart';

class WaultTheme {
  WaultTheme._();

  static ThemeData get darkTheme {
    final TextTheme baseTextTheme = GoogleFonts.interTextTheme(
      ThemeData.dark().textTheme,
    );

    final TextTheme textTheme = baseTextTheme.copyWith(
      displayLarge: baseTextTheme.displayLarge?.copyWith(
        color: WaultColors.textPrimary,
      ),
      displayMedium: baseTextTheme.displayMedium?.copyWith(
        color: WaultColors.textPrimary,
      ),
      displaySmall: baseTextTheme.displaySmall?.copyWith(
        color: WaultColors.textPrimary,
      ),
      headlineLarge: baseTextTheme.headlineLarge?.copyWith(
        color: WaultColors.textPrimary,
      ),
      headlineMedium: baseTextTheme.headlineMedium?.copyWith(
        color: WaultColors.textPrimary,
      ),
      headlineSmall: baseTextTheme.headlineSmall?.copyWith(
        color: WaultColors.textPrimary,
      ),
      titleLarge: baseTextTheme.titleLarge?.copyWith(
        color: WaultColors.textPrimary,
        fontWeight: FontWeight.w600,
      ),
      titleMedium: baseTextTheme.titleMedium?.copyWith(
        color: WaultColors.textPrimary,
        fontWeight: FontWeight.w500,
      ),
      titleSmall: baseTextTheme.titleSmall?.copyWith(
        color: WaultColors.textSecondary,
        fontWeight: FontWeight.w500,
      ),
      bodyLarge: baseTextTheme.bodyLarge?.copyWith(
        color: WaultColors.textPrimary,
      ),
      bodyMedium: baseTextTheme.bodyMedium?.copyWith(
        color: WaultColors.textSecondary,
      ),
      bodySmall: baseTextTheme.bodySmall?.copyWith(
        color: WaultColors.textTertiary,
      ),
      labelLarge: baseTextTheme.labelLarge?.copyWith(
        color: WaultColors.textPrimary,
        fontWeight: FontWeight.w600,
      ),
      labelMedium: baseTextTheme.labelMedium?.copyWith(
        color: WaultColors.textSecondary,
      ),
      labelSmall: baseTextTheme.labelSmall?.copyWith(
        color: WaultColors.textTertiary,
      ),
    );

    final ColorScheme colorScheme = ColorScheme.dark(
      brightness: Brightness.dark,
      primary: WaultColors.primary,
      onPrimary: WaultColors.background,
      secondary: WaultColors.activeBlue,
      onSecondary: WaultColors.background,
      error: WaultColors.error,
      onError: WaultColors.textPrimary,
      surface: WaultColors.surface,
      onSurface: WaultColors.textPrimary,
      surfaceContainerHighest: WaultColors.surfaceElevated,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: WaultColors.background,
      textTheme: textTheme,
      appBarTheme: AppBarTheme(
        backgroundColor: WaultColors.background,
        foregroundColor: WaultColors.textPrimary,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: textTheme.titleLarge,
      ),
      cardTheme: CardThemeData(
        color: WaultColors.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16.0),
          side: const BorderSide(color: WaultColors.glassBorder, width: 1.0),
        ),
        margin: EdgeInsets.zero,
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: WaultColors.surface,
        modalBackgroundColor: WaultColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20.0)),
        ),
        modalBarrierColor: Colors.black54,
      ),
      dividerTheme: const DividerThemeData(
        color: WaultColors.divider,
        thickness: 0.5,
        space: 0,
      ),
      dividerColor: WaultColors.divider,
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: WaultColors.surfaceElevated,
        hintStyle: textTheme.bodyMedium?.copyWith(
          color: WaultColors.textTertiary,
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16.0,
          vertical: 14.0,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: const BorderSide(color: WaultColors.glassBorder),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: const BorderSide(color: WaultColors.glassBorder),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: const BorderSide(color: WaultColors.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: const BorderSide(color: WaultColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: const BorderSide(color: WaultColors.error, width: 1.5),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: WaultColors.primary,
        foregroundColor: WaultColors.background,
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16.0),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: WaultColors.surfaceElevated,
        contentTextStyle: textTheme.bodyMedium?.copyWith(
          color: WaultColors.textPrimary,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12.0),
        ),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}
