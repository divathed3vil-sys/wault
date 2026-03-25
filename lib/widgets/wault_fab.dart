// File: lib/widgets/wault_fab.dart
import 'dart:math';

import 'package:flutter/material.dart';

import '../theme/wault_colors.dart';

class WaultFab extends StatefulWidget {
  final VoidCallback onPressed;
  final String? tooltip;

  const WaultFab({super.key, required this.onPressed, this.tooltip});

  @override
  State<WaultFab> createState() => _WaultFabState();
}

class _WaultFabState extends State<WaultFab>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 8),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const double buttonSize = 56.0;
    const double ringSize = 72.0;

    Widget fab = SizedBox(
      width: ringSize,
      height: ringSize,
      child: Stack(
        alignment: Alignment.center,
        children: <Widget>[
          AnimatedBuilder(
            animation: _controller,
            builder: (BuildContext context, Widget? child) {
              return Transform.rotate(
                angle: _controller.value * 2 * pi,
                child: child,
              );
            },
            child: Container(
              width: ringSize,
              height: ringSize,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: SweepGradient(
                  colors: <Color>[
                    WaultColors.primary.withOpacity(0.4),
                    WaultColors.primaryGlow,
                    WaultColors.primary.withOpacity(0.1),
                    WaultColors.primaryGlow,
                    WaultColors.primary.withOpacity(0.4),
                  ],
                  stops: const <double>[0.0, 0.25, 0.5, 0.75, 1.0],
                ),
              ),
              child: Center(
                child: Container(
                  width: ringSize - 4,
                  height: ringSize - 4,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: WaultColors.background,
                  ),
                ),
              ),
            ),
          ),
          Material(
            color: WaultColors.primary,
            shape: const CircleBorder(),
            elevation: 6.0,
            shadowColor: WaultColors.primaryGlow,
            child: InkWell(
              onTap: widget.onPressed,
              customBorder: const CircleBorder(),
              splashColor: Colors.white24,
              highlightColor: Colors.white10,
              child: const SizedBox(
                width: buttonSize,
                height: buttonSize,
                child: Icon(
                  Icons.add_rounded,
                  color: WaultColors.background,
                  size: 28.0,
                ),
              ),
            ),
          ),
        ],
      ),
    );

    if (widget.tooltip != null) {
      fab = Tooltip(message: widget.tooltip!, child: fab);
    }

    return fab;
  }
}
