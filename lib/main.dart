// File: lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'screens/splash_screen.dart';
import 'screens/vault_screen.dart';
import 'services/engine_service.dart';
import 'theme/wault_theme.dart';
import 'utils/constants.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await SystemChrome.setPreferredOrientations(<DeviceOrientation>[
    DeviceOrientation.portraitUp,
  ]);

  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.black,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );

  await EngineService.instance.initialize();

  runApp(const WaultApp());
}

class WaultApp extends StatelessWidget {
  const WaultApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: WaultConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: WaultTheme.darkTheme,
      home: const AppNavigator(),
    );
  }
}

class AppNavigator extends StatefulWidget {
  const AppNavigator({super.key});

  @override
  State<AppNavigator> createState() => _AppNavigatorState();
}

class _AppNavigatorState extends State<AppNavigator> {
  bool _showSplash = true;

  void _completeSplash() {
    if (!mounted) return;
    setState(() {
      _showSplash = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_showSplash) {
      return SplashScreen(onFinished: _completeSplash);
    }

    return const VaultScreen();
  }
}
