class Account {
  final String id;
  final String label;
  final String accentColorHex;
  final int processSlot;
  final int createdAt;
  final int lastActiveAt;
  final String state;
  final int unreadCount;
  final int sortOrder;
  final String? snapshotPath;
  final int? snapshotTimestamp;
  final double scrollPositionY;
  final bool hasNotification;
  final int totalInteractions;

  const Account({
    required this.id,
    required this.label,
    required this.accentColorHex,
    required this.processSlot,
    required this.createdAt,
    required this.lastActiveAt,
    this.state = 'COLD',
    this.unreadCount = 0,
    required this.sortOrder,
    this.snapshotPath,
    this.snapshotTimestamp,
    this.scrollPositionY = 0.0,
    this.hasNotification = false,
    this.totalInteractions = 0,
  });

  factory Account.fromJson(Map<String, dynamic> json) {
    return Account(
      id: json['id'] as String? ?? '',
      label: json['label'] as String? ?? '',
      accentColorHex: json['accentColorHex'] as String? ?? '#25D366',
      processSlot: _parseInt(json['processSlot'], 0),
      createdAt: _parseInt(json['createdAt'], 0),
      lastActiveAt: _parseInt(json['lastActiveAt'], 0),
      state: json['state'] as String? ?? 'COLD',
      unreadCount: _parseInt(json['unreadCount'], 0),
      sortOrder: _parseInt(json['sortOrder'], 0),
      snapshotPath: json['snapshotPath'] as String?,
      snapshotTimestamp: json['snapshotTimestamp'] != null
          ? _parseInt(json['snapshotTimestamp'], 0)
          : null,
      scrollPositionY: _parseDouble(json['scrollPositionY'], 0.0),
      hasNotification: json['hasNotification'] as bool? ?? false,
      totalInteractions: _parseInt(json['totalInteractions'], 0),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'label': label,
      'accentColorHex': accentColorHex,
      'processSlot': processSlot,
      'createdAt': createdAt,
      'lastActiveAt': lastActiveAt,
      'state': state,
      'unreadCount': unreadCount,
      'sortOrder': sortOrder,
      'snapshotPath': snapshotPath,
      'snapshotTimestamp': snapshotTimestamp,
      'scrollPositionY': scrollPositionY,
      'hasNotification': hasNotification,
      'totalInteractions': totalInteractions,
    };
  }

  Account copyWith({
    String? id,
    String? label,
    String? accentColorHex,
    int? processSlot,
    int? createdAt,
    int? lastActiveAt,
    String? state,
    int? unreadCount,
    int? sortOrder,
    String? snapshotPath,
    int? snapshotTimestamp,
    double? scrollPositionY,
    bool? hasNotification,
    int? totalInteractions,
  }) {
    return Account(
      id: id ?? this.id,
      label: label ?? this.label,
      accentColorHex: accentColorHex ?? this.accentColorHex,
      processSlot: processSlot ?? this.processSlot,
      createdAt: createdAt ?? this.createdAt,
      lastActiveAt: lastActiveAt ?? this.lastActiveAt,
      state: state ?? this.state,
      unreadCount: unreadCount ?? this.unreadCount,
      sortOrder: sortOrder ?? this.sortOrder,
      snapshotPath: snapshotPath ?? this.snapshotPath,
      snapshotTimestamp: snapshotTimestamp ?? this.snapshotTimestamp,
      scrollPositionY: scrollPositionY ?? this.scrollPositionY,
      hasNotification: hasNotification ?? this.hasNotification,
      totalInteractions: totalInteractions ?? this.totalInteractions,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Account && other.id == id;
  }

  @override
  int get hashCode => id.hashCode;

  @override
  String toString() => 'Account(id: $id, label: $label, state: $state)';

  static int _parseInt(dynamic value, int fallback) {
    if (value is int) return value;
    if (value is double) return value.toInt();
    if (value is String) return int.tryParse(value) ?? fallback;
    return fallback;
  }

  static double _parseDouble(dynamic value, double fallback) {
    if (value is double) return value;
    if (value is int) return value.toDouble();
    if (value is String) return double.tryParse(value) ?? fallback;
    return fallback;
  }
}
