// lib/services/engine_service.dart

import 'dart:async';

import 'package:flutter/services.dart';
import 'package:wault/models/account.dart';

class SessionEvent {
  final String type;
  final String accountId;
  final int? count;
  final String? state;
  final String? message;

  const SessionEvent({
    required this.type,
    required this.accountId,
    this.count,
    this.state,
    this.message,
  });

  factory SessionEvent.fromMap(Map<dynamic, dynamic> map) {
    return SessionEvent(
      type: map['type']?.toString() ?? '',
      accountId: map['accountId']?.toString() ?? '',
      count: map['count'] is int ? map['count'] as int : null,
      state: map['state']?.toString(),
      message: map['message']?.toString(),
    );
  }

  bool get isUnreadCount => type == 'unreadCount';
  bool get isQrVisible => type == 'qrVisible';
  bool get isLoggedIn => type == 'loggedIn';
  bool get isSessionCrashed => type == 'sessionCrashed';
  bool get isStateChanged => type == 'sessionStateChanged';
  bool get isSessionError => type == 'sessionError';
}

class EngineService {
  EngineService._internal();

  static final EngineService instance = EngineService._internal();

  static const MethodChannel _methodChannel = MethodChannel(
    'com.diva.wault/engine',
  );
  static const EventChannel _eventChannel = EventChannel(
    'com.diva.wault/events',
  );

  final StreamController<SessionEvent> _sessionEventsController =
      StreamController<SessionEvent>.broadcast();

  StreamSubscription<dynamic>? _eventSubscription;
  bool _initialized = false;

  Stream<SessionEvent> get sessionEvents => _sessionEventsController.stream;

  void initialize() {
    if (_initialized) return;
    _initialized = true;

    _eventSubscription = _eventChannel.receiveBroadcastStream().listen((
      dynamic event,
    ) {
      if (event is Map) {
        _sessionEventsController.add(SessionEvent.fromMap(event));
      }
    }, onError: (_) {});
  }

  void dispose() {
    _eventSubscription?.cancel();
    _eventSubscription = null;
    _initialized = false;
  }

  Future<bool> openSession(Account account) async {
    try {
      await _methodChannel.invokeMethod<void>('openSession', <String, dynamic>{
        'accountId': account.id,
        'label': account.label,
        'accentColor': account.accentColorHex,
        'processSlot': account.processSlot,
      });
      return true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> closeSession(String accountId) async {
    try {
      await _methodChannel.invokeMethod<void>('closeSession', <String, dynamic>{
        'accountId': accountId,
      });
      return true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> closeAllSessions() async {
    try {
      await _methodChannel.invokeMethod<void>('closeAllSessions');
      return true;
    } on PlatformException {
      return false;
    }
  }

  Future<Map<String, dynamic>> getDeviceInfo() async {
    try {
      final result = await _methodChannel.invokeMethod<dynamic>(
        'getDeviceInfo',
      );
      if (result is Map) {
        return Map<String, dynamic>.from(result);
      }
      return <String, dynamic>{};
    } on PlatformException {
      return <String, dynamic>{};
    }
  }

  Future<String?> captureSnapshot(String accountId) async {
    try {
      final result = await _methodChannel.invokeMethod<dynamic>(
        'captureSnapshot',
        <String, dynamic>{'accountId': accountId},
      );
      return result?.toString();
    } on PlatformException {
      return null;
    }
  }
}
