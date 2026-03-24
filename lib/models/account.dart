// lib/models/account.dart

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
      id: json['id'] as String? ?? '',
      label: json['label'] as String? ?? '',
      accentColorHex: json['accentColorHex'] as String? ?? '#25D366',
      processSlot: json['processSlot'] as int? ?? 0,
      createdAt: json['createdAt'] as int? ?? 0,
      lastActiveAt: json['lastActiveAt'] as int? ?? 0,
      state: json['state'] as String? ?? 'COLD',
      unreadCount: json['unreadCount'] as int? ?? 0,
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
}
