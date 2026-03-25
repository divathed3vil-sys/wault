// File: lib/models/account.dart
class Account {
  final String id;
  final String label;
  final String accentColorHex;
  final int processSlot;
  final int createdAt;
  final int lastActiveAt;
  final String state;
  final int unreadCount;

  const Account({
    required this.id,
    required this.label,
    required this.accentColorHex,
    required this.processSlot,
    required this.createdAt,
    required this.lastActiveAt,
    required this.state,
    required this.unreadCount,
  });

  factory Account.fromJson(Map<String, dynamic> json) {
    return Account(
      id: (json['id'] ?? '').toString(),
      label: (json['label'] ?? '').toString(),
      accentColorHex: (json['accentColorHex'] ?? '#25D366').toString(),
      processSlot: _asInt(json['processSlot']),
      createdAt: _asInt(json['createdAt']),
      lastActiveAt: _asInt(json['lastActiveAt']),
      state: (json['state'] ?? 'COLD').toString(),
      unreadCount: _asInt(json['unreadCount']),
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'id': id,
      'label': label,
      'accentColorHex': accentColorHex,
      'processSlot': processSlot,
      'createdAt': createdAt,
      'lastActiveAt': lastActiveAt,
      'state': state,
      'unreadCount': unreadCount,
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
    );
  }

  static int _asInt(dynamic value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
