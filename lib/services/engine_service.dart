// File: lib/services/engine_service.dart
import 'dart:async';

import 'package:flutter/services.dart';

import '../models/account.dart';
import '../utils/constants.dart';

class EngineService {
  EngineService._();

  static final EngineService instance = EngineService._();

  final MethodChannel _methodChannel = const MethodChannel(
    WaultChannels.engine,
  );

  final EventChannel _eventChannel = const EventChannel(WaultChannels.events);

  final StreamController<Map<String, dynamic>> _sessionEventsController =
      StreamController<Map<String, dynamic>>.broadcast();

  StreamSubscription<dynamic>? _eventSubscription;
  bool _initialized = false;

  Stream<Map<String, dynamic>> get sessionEvents =>
      _sessionEventsController.stream;

  Future<void> initialize() async {
    if (_initialized) return;

    _eventSubscription = _eventChannel.receiveBroadcastStream().listen(
      (dynamic event) {
        if (event is Map) {
          final Map<String, dynamic> normalized = Map<String, dynamic>.from(
            event.map(
              (dynamic key, dynamic value) => MapEntry(key.toString(), value),
            ),
          );

          if (!_sessionEventsController.isClosed) {
            _sessionEventsController.add(normalized);
          }
        }
      },
      onError: (Object error, StackTrace stackTrace) {
        if (!_sessionEventsController.isClosed) {
          _sessionEventsController.add(<String, dynamic>{
            'type': 'sessionError',
            'message': error.toString(),
          });
        }
      },
    );

    _initialized = true;
  }

  Future<void> dispose() async {
    await _eventSubscription?.cancel();
    _eventSubscription = null;
    _initialized = false;
  }

  Future<void> openSession(Account account) async {
    await _methodChannel
        .invokeMethod<void>(WaultMethods.openSession, <String, dynamic>{
          'accountId': account.id,
          'label': account.label,
          'accentColor': account.accentColorHex,
          'processSlot': account.processSlot,
        });
  }

  Future<void> closeSession(String accountId) async {
    await _methodChannel.invokeMethod<void>(
      WaultMethods.closeSession,
      <String, dynamic>{'accountId': accountId},
    );
  }

  Future<void> closeAllSessions() async {
    await _methodChannel.invokeMethod<void>(WaultMethods.closeAllSessions);
  }

  Future<Map<String, dynamic>> getDeviceInfo() async {
    final dynamic result = await _methodChannel.invokeMethod<dynamic>(
      WaultMethods.getDeviceInfo,
    );

    if (result is Map) {
      return Map<String, dynamic>.from(
        result.map(
          (dynamic key, dynamic value) => MapEntry(key.toString(), value),
        ),
      );
    }

    return <String, dynamic>{};
  }

  Future<String?> captureSnapshot(String accountId) async {
    final dynamic result = await _methodChannel.invokeMethod<dynamic>(
      WaultMethods.captureSnapshot,
      <String, dynamic>{'accountId': accountId},
    );

    if (result == null) {
      return null;
    }

    return result.toString();
  }
}
