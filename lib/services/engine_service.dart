import 'dart:async';

import 'package:flutter/services.dart';
import 'package:wault/utils/constants.dart';

class EngineService {
  EngineService._internal();

  static final EngineService instance = EngineService._internal();

  final MethodChannel _methodChannel = const MethodChannel(
    WaultChannels.engine,
  );
  final EventChannel _eventChannel = const EventChannel(WaultChannels.events);

  Stream<Map<String, dynamic>>? _eventStream;

  Stream<Map<String, dynamic>> get events {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map(
      (event) => _parseEvent(event),
    );
    return _eventStream!;
  }

  Future<void> openSession({
    required String accountId,
    required String label,
    required String accentColor,
    required int processSlot,
  }) async {
    try {
      await _methodChannel.invokeMethod(WaultMethods.openSession, {
        'accountId': accountId,
        'label': label,
        'accentColor': accentColor,
        'processSlot': processSlot,
      });
    } on PlatformException {
      return;
    }
  }

  Future<void> closeSession(String accountId) async {
    try {
      await _methodChannel.invokeMethod(WaultMethods.closeSession, {
        'accountId': accountId,
      });
    } on PlatformException {
      return;
    }
  }

  Future<void> closeAllSessions() async {
    try {
      await _methodChannel.invokeMethod(WaultMethods.closeAllSessions);
    } on PlatformException {
      return;
    }
  }

  Future<Map<String, dynamic>> getDeviceInfo() async {
    try {
      final result = await _methodChannel.invokeMethod<Map<dynamic, dynamic>>(
        WaultMethods.getDeviceInfo,
      );
      if (result == null) {
        return {};
      }
      return Map<String, dynamic>.from(result);
    } on PlatformException {
      return {};
    }
  }

  Future<String?> captureSnapshot(String accountId) async {
    try {
      final result = await _methodChannel.invokeMethod<String>(
        WaultMethods.captureSnapshot,
        {'accountId': accountId},
      );
      return result;
    } on PlatformException {
      return null;
    }
  }

  Map<String, dynamic> _parseEvent(dynamic event) {
    if (event is Map) {
      return Map<String, dynamic>.from(event);
    }
    return {'raw': event};
  }
}
