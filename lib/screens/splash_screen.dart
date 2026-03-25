// lib/screens/splash_screen.dart

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/widgets/shield_logo.dart';

class SplashScreen extends StatefulWidget {
  final VoidCallback? onFinished;

  const SplashScreen({super.key, this.onFinished});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with TickerProviderStateMixin {
  late final AnimationController _logoController;
  late final AnimationController _titleController;
  late final AnimationController _subtitleController;
  late final AnimationController _exitController;

  late final Animation<double> _logoOpacity;
  late final Animation<double> _logoScale;
  late final Animation<double> _titleOpacity;
  late final Animation<double> _subtitleOpacity;
  late final Animation<double> _exitOpacity;
  late final Animation<double> _exitScale;

  Timer? _exitTimer;

  @override
  void initState() {
    super.initState();

    _logoController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _titleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _subtitleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _exitController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );

    _logoOpacity = CurvedAnimation(
      parent: _logoController,
      curve: Curves.easeOut,
    );

    _logoScale = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(parent: _logoController, curve: Curves.elasticOut),
    );

    _titleOpacity = CurvedAnimation(
      parent: _titleController,
      curve: Curves.easeOut,
    );

    _subtitleOpacity = CurvedAnimation(
      parent: _subtitleController,
      curve: Curves.easeOut,
    );

    _exitOpacity = Tween<double>(
      begin: 1.0,
      end: 0.0,
    ).animate(CurvedAnimation(parent: _exitController, curve: Curves.easeOut));

    _exitScale = Tween<double>(
      begin: 1.0,
      end: 1.1,
    ).animate(CurvedAnimation(parent: _exitController, curve: Curves.easeOut));

    _runSequence();
  }

  Future<void> _runSequence() async {
    await Future.delayed(const Duration(milliseconds: 200));
    if (!mounted) return;

    _logoController.forward();

    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;

    _titleController.forward();

    await Future.delayed(const Duration(milliseconds: 400));
    if (!mounted) return;

    _subtitleController.forward();

    _exitTimer = Timer(const Duration(milliseconds: 1200), () async {
      if (!mounted) return;
      await _exitController.forward();
      if (!mounted) return;
      widget.onFinished?.call();
    });
  }

  @override
  void dispose() {
    _exitTimer?.cancel();
    _logoController.dispose();
    _titleController.dispose();
    _subtitleController.dispose();
    _exitController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: WaultColors.background,
      body: Center(
        child: AnimatedBuilder(
          animation: Listenable.merge([
            _logoController,
            _titleController,
            _subtitleController,
            _exitController,
          ]),
          builder: (context, child) {
            return Opacity(
              opacity: _exitOpacity.value,
              child: Transform.scale(
                scale: _exitScale.value,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Opacity(
                      opacity: _logoOpacity.value,
                      child: Transform.scale(
                        scale: _logoScale.value,
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            Container(
                              width: 140,
                              height: 140,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                gradient: RadialGradient(
                                  colors: [
                                    WaultColors.primary.withOpacity(0.15),
                                    Colors.transparent,
                                  ],
                                ),
                              ),
                            ),
                            const ShieldLogo(
                              size: 90,
                              useAssetIfAvailable: true,
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    Opacity(
                      opacity: _titleOpacity.value,
                      child: const Text(
                        'WAult',
                        style: TextStyle(
                          color: WaultColors.textPrimary,
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 2,
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Opacity(
                      opacity: _subtitleOpacity.value,
                      child: const Text(
                        'a project by DIVA',
                        style: TextStyle(
                          color: WaultColors.textSecondary,
                          fontSize: 15,
                          letterSpacing: 1,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
