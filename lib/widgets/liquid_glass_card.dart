// File: lib/widgets/liquid_glass_card.dart
import 'package:flutter/material.dart';

import '../theme/wault_colors.dart';

class LiquidGlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double borderRadius;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final Color? accentColor;
  final bool showGlow;

  const LiquidGlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.borderRadius = 20.0,
    this.onTap,
    this.onLongPress,
    this.accentColor,
    this.showGlow = false,
  });

  @override
  Widget build(BuildContext context) {
    final bool hasInteraction = onTap != null || onLongPress != null;
    final BorderRadius effectiveRadius = BorderRadius.circular(borderRadius);

    Widget cardContent = Container(
      decoration: BoxDecoration(
        borderRadius: effectiveRadius,
        color: WaultColors.surface,
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[
            WaultColors.glassHighlight,
            WaultColors.glassWhite,
            Colors.transparent,
          ],
          stops: <double>[0.0, 0.3, 1.0],
        ),
        border: Border.all(color: WaultColors.glassBorder, width: 1.0),
        boxShadow: _buildShadows(),
      ),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: effectiveRadius,
          color: WaultColors.surface.withOpacity(0.85),
        ),
        padding: padding,
        child: child,
      ),
    );

    if (hasInteraction) {
      cardContent = Material(
        color: Colors.transparent,
        borderRadius: effectiveRadius,
        child: InkWell(
          onTap: onTap,
          onLongPress: onLongPress,
          borderRadius: effectiveRadius,
          splashColor: WaultColors.glassWhite,
          highlightColor: WaultColors.glassHighlight,
          child: cardContent,
        ),
      );
    }

    if (margin != null) {
      cardContent = Padding(padding: margin!, child: cardContent);
    }

    return cardContent;
  }

  List<BoxShadow> _buildShadows() {
    final List<BoxShadow> shadows = <BoxShadow>[
      BoxShadow(
        color: Colors.black.withOpacity(0.30),
        blurRadius: 8.0,
        offset: const Offset(0, 4),
      ),
    ];

    if (showGlow && accentColor != null) {
      shadows.add(
        BoxShadow(
          color: accentColor!.withOpacity(0.25),
          blurRadius: 16.0,
          spreadRadius: 1.0,
          offset: Offset.zero,
        ),
      );
    }

    return shadows;
  }
}
