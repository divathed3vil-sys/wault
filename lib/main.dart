import 'package:flutter/material.dart';
import 'package:wault/theme/wault_theme.dart';
import 'package:wault/screens/splash_screen.dart';
import 'package:wault/screens/vault_screen.dart';
import 'package:wault/screens/settings_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const WaultApp());
}

class WaultApp extends StatelessWidget {
  const WaultApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WAult',
      debugShowCheckedModeBanner: false,
      theme: WaultTheme.darkTheme,
      home: const _AppShell(),
    );
  }
}

class _AppShell extends StatefulWidget {
  const _AppShell();

  @override
  State<_AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<_AppShell> {
  bool _splashDone = false;

  void _onSplashFinished() {
    if (!mounted) return;
    setState(() {
      _splashDone = true;
    });
  }

  void _openSettings() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (context) =>
            SettingsScreen(onBack: () => Navigator.of(context).pop()),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!_splashDone) {
      return SplashScreen(onFinished: _onSplashFinished);
    }

    return VaultScreen(onOpenSettings: _openSettings);
  }
}
