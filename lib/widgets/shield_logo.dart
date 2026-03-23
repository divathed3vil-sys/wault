//import 'dart:math';

import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';

class ShieldLogo extends StatelessWidget {
  final double size;
  final double opacity;

  const ShieldLogo({super.key, required this.size, this.opacity = 1.0});

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: opacity,
      child: CustomPaint(
        size: Size(size, size * 1.15),
        painter: _ShieldLogoPainter(size: size),
      ),
    );
  }
}

class _ShieldLogoPainter extends CustomPainter {
  final double size;

  _ShieldLogoPainter({required this.size});

  @override
  void paint(Canvas canvas, Size canvasSize) {
    final width = canvasSize.width;
    final height = canvasSize.height;
    final centerX = width / 2;

    final path = _buildShieldPath(width, height, centerX);

    _drawGradientFill(canvas, path, width, height);
    _drawInnerGlow(canvas, path, centerX, height);
    _drawLetter(canvas, centerX, height);
  }

  Path _buildShieldPath(double width, double height, double centerX) {
    final path = Path();

    final topY = height * 0.02;
    final shoulderY = height * 0.18;
    final midY = height * 0.55;
    final bottomY = height * 0.98;

    final topInset = width * 0.15;
    final shoulderWidth = width * 0.48;
    final midWidth = width * 0.42;

    path.moveTo(centerX, topY);
    path.lineTo(centerX + topInset, topY);
    path.lineTo(centerX + shoulderWidth, shoulderY);
    path.lineTo(centerX + midWidth, midY);
    path.lineTo(centerX, bottomY);
    path.lineTo(centerX - midWidth, midY);
    path.lineTo(centerX - shoulderWidth, shoulderY);
    path.lineTo(centerX - topInset, topY);
    path.close();

    return path;
  }

  void _drawGradientFill(
    Canvas canvas,
    Path path,
    double width,
    double height,
  ) {
    final gradient = LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [WaultColors.primary, WaultColors.primary.withValues(alpha: 0.7)],
    );

    final rect = Rect.fromLTWH(0, 0, width, height);
    final paint = Paint()
      ..shader = gradient.createShader(rect)
      ..style = PaintingStyle.fill;

    canvas.drawPath(path, paint);
  }

  void _drawInnerGlow(Canvas canvas, Path path, double centerX, double height) {
    final glowCenter = Offset(centerX, height * 0.35);
    final glowRadius = size * 0.5;

    final gradient = RadialGradient(
      center: Alignment.center,
      radius: 1.0,
      colors: [
        Colors.white.withValues(alpha: 0.2),
        Colors.white.withValues(alpha: 0.0),
      ],
    );

    final rect = Rect.fromCircle(center: glowCenter, radius: glowRadius);
    final paint = Paint()
      ..shader = gradient.createShader(rect)
      ..style = PaintingStyle.fill;

    canvas.save();
    canvas.clipPath(path);
    canvas.drawCircle(glowCenter, glowRadius, paint);
    canvas.restore();
  }

  void _drawLetter(Canvas canvas, double centerX, double height) {
    final fontSize = size * 0.38;
    final letterY = height * 0.42;

    final textSpan = TextSpan(
      text: 'W',
      style: TextStyle(
        color: WaultColors.textPrimary,
        fontSize: fontSize,
        fontWeight: FontWeight.w700,
        letterSpacing: -1.0,
      ),
    );

    final textPainter = TextPainter(
      text: textSpan,
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
    );

    textPainter.layout();

    final textX = centerX - (textPainter.width / 2);
    final textY = letterY - (textPainter.height / 2);

    textPainter.paint(canvas, Offset(textX, textY));
  }

  @override
  bool shouldRepaint(covariant _ShieldLogoPainter oldDelegate) {
    return oldDelegate.size != size;
  }
}
