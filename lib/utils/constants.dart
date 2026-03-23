class WaultConstants {
  WaultConstants._();

  static const String appName = 'WAult';
  static const String appSubtitle = 'a project by DIVA';
  static const String packageName = 'com.diva.wault';

  static const int minSlot = 0;
  static const int maxSlot = 4;
  static const int maxProcessSlots = 5;
  static const int defaultMaxAccounts = 5;

  static const int splashDurationMs = 1800;
  static const int splashFadeDurationMs = 400;
}

class WaultChannels {
  WaultChannels._();

  static const String engine = 'com.diva.wault/engine';
  static const String events = 'com.diva.wault/events';
}

class WaultMethods {
  WaultMethods._();

  static const String openSession = 'openSession';
  static const String closeSession = 'closeSession';
  static const String closeAllSessions = 'closeAllSessions';
  static const String getDeviceInfo = 'getDeviceInfo';
  static const String captureSnapshot = 'captureSnapshot';
}

class WaultSizes {
  WaultSizes._();

  static const double paddingXs = 4.0;
  static const double paddingSm = 8.0;
  static const double paddingMd = 16.0;
  static const double paddingLg = 24.0;
  static const double paddingXl = 32.0;

  static const double cardRadius = 16.0;
  static const double cardRadiusSm = 12.0;

  static const double fabSize = 56.0;
  static const double topBarHeight = 56.0;
  static const double topBarSpacing = 16.0;
}

class WaultAccentHex {
  WaultAccentHex._();

  static const List<String> palette = [
    '#25D366',
    '#53BDEB',
    '#FF6B9D',
    '#FFB340',
    '#A78BFA',
    '#34D399',
    '#F472B6',
    '#60A5FA',
  ];
}
