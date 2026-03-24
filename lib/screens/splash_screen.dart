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

  late final Animation<double> _logoFade;
  late final Animation<double> _logoScale;
  late final Animation<double> _titleFade;
  late final Animation<double> _subtitleFade;
  late final Animation<double> _exitFade;

  @override
  void initState() {
    super.initState();

    _logoController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _titleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _subtitleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _exitController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );

    _logoFade = CurvedAnimation(parent: _logoController, curve: Curves.easeOut);
    _logoScale = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(parent: _logoController, curve: Curves.easeOutBack),
    );
    _titleFade = CurvedAnimation(
      parent: _titleController,
      curve: Curves.easeOut,
    );
    _subtitleFade = CurvedAnimation(
      parent: _subtitleController,
      curve: Curves.easeOut,
    );
    _exitFade = Tween<double>(
      begin: 1.0,
      end: 0.0,
    ).animate(CurvedAnimation(parent: _exitController, curve: Curves.easeIn));

    _startSequence();
  }

  Future<void> _startSequence() async {
    await Future.delayed(const Duration(milliseconds: 200));
    if (!mounted) return;

    await _logoController.forward();
    if (!mounted) return;

    await Future.delayed(const Duration(milliseconds: 150));
    if (!mounted) return;

    await _titleController.forward();
    if (!mounted) return;

    await Future.delayed(const Duration(milliseconds: 100));
    if (!mounted) return;

    await _subtitleController.forward();
    if (!mounted) return;

    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;

    await _exitController.forward();
    if (!mounted) return;

    widget.onFinished?.call();
  }

  @override
  void dispose() {
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
      body: AnimatedBuilder(
        animation: _exitController,
        builder: (context, child) {
          return Opacity(opacity: _exitFade.value, child: child);
        },
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AnimatedBuilder(
                animation: _logoController,
                builder: (context, child) {
                  return Opacity(
                    opacity: _logoFade.value,
                    child: Transform.scale(
                      scale: _logoScale.value,
                      child: child,
                    ),
                  );
                },
                child: const ShieldLogo(size: 100.0),
              ),
              const SizedBox(height: 28.0),
              FadeTransition(
                opacity: _titleFade,
                child: Text(
                  'WAult',
                  style: TextStyle(
                    color: WaultColors.textPrimary,
                    fontSize: 32.0,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1.2,
                  ),
                ),
              ),
              const SizedBox(height: 8.0),
              FadeTransition(
                opacity: _subtitleFade,
                child: Text(
                  'a project by DIVA',
                  style: TextStyle(
                    color: WaultColors.textSecondary,
                    fontSize: 14.0,
                    fontWeight: FontWeight.w400,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class AnimatedBuilder extends AnimatedWidget {
  final Widget? child;
  final Widget Function(BuildContext context, Widget? child) builder;

  const AnimatedBuilder({
    super.key,
    required Animation<double> animation,
    required this.builder,
    this.child,
  }) : super(listenable: animation);

  @override
  Widget build(BuildContext context) {
    return builder(context, child);
  }
}
