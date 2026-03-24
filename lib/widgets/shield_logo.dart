// lib/widgets/shield_logo.dart

import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';

class ShieldLogo extends StatelessWidget {
  final double size;
  final double opacity;
  final String? assetPath;
  final bool useAssetIfAvailable;

  const ShieldLogo({
    super.key,
    this.size = 64,
    this.opacity = 1.0,
    this.assetPath,
    this.useAssetIfAvailable = false,
  });

  @override
  Widget build(BuildContext context) {
    final effectiveAssetPath = assetPath ?? 'assets/images/wault_logo.png';

    final child =
        useAssetIfAvailable
            ? Image.asset(
              effectiveAssetPath,
              width: size,
              height: size,
              fit: BoxFit.contain,
              errorBuilder:
                  (_, __, ___) => CustomPaint(
                    size: Size.square(size),
                    painter: _ShieldLogoPainter(),
                  ),
            )
            : CustomPaint(
              size: Size.square(size),
              painter: _ShieldLogoPainter(),
            );

    return Opacity(opacity: opacity, child: child);
  }
}

class _ShieldLogoPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final rect = Offset.zero & size;

    final fillPaint =
        Paint()
          ..shader = const LinearGradient(
            colors: [WaultColors.primary, Color(0xB325D366)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ).createShader(rect);

    final highlightPaint =
        Paint()
          ..shader = RadialGradient(
            colors: [
              Colors.white.withOpacity(0.20),
              Colors.white.withOpacity(0.0),
            ],
            center: const Alignment(-0.2, -0.3),
            radius: 0.8,
          ).createShader(rect);

    final borderPaint =
        Paint()
          ..color = Colors.white.withOpacity(0.10)
          ..style = PaintingStyle.stroke
          ..strokeWidth = size.width * 0.025;

    final path = Path();
    path.moveTo(size.width * 0.50, size.height * 0.04);
    path.lineTo(size.width * 0.86, size.height * 0.18);
    path.lineTo(size.width * 0.86, size.height * 0.56);
    path.quadraticBezierTo(
      size.width * 0.86,
      size.height * 0.84,
      size.width * 0.50,
      size.height * 0.96,
    );
    path.quadraticBezierTo(
      size.width * 0.14,
      size.height * 0.84,
      size.width * 0.14,
      size.height * 0.56,
    );
    path.lineTo(size.width * 0.14, size.height * 0.18);
    path.close();

    canvas.drawPath(path, fillPaint);
    canvas.drawPath(path, highlightPaint);
    canvas.drawPath(path, borderPaint);

    final textPainter = TextPainter(
      text: TextSpan(
        text: 'W',
        style: TextStyle(
          color: Colors.white,
          fontSize: size.width * 0.38,
          fontWeight: FontWeight.w800,
          letterSpacing: 1,
        ),
      ),
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
    )..layout();

    final textOffset = Offset(
      (size.width - textPainter.width) / 2,
      (size.height - textPainter.height) / 2 - size.height * 0.02,
    );

    textPainter.paint(canvas, textOffset);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
