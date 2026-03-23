import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';
import 'package:wault/models/account.dart';

class AccountService {
  static const String _fileName = 'accounts.json';
  static const int _minSlot = 0;
  static const int _maxSlot = 4;

  final Uuid _uuid = const Uuid();

  Future<File> _getFile() async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/$_fileName');
  }

  Future<List<Account>> loadAccounts() async {
    try {
      final file = await _getFile();
      if (!await file.exists()) {
        return [];
      }

      final contents = await file.readAsString();
      if (contents.isEmpty) {
        return [];
      }

      final dynamic decoded = jsonDecode(contents);
      if (decoded is! List) {
        return [];
      }

      final accounts = <Account>[];
      for (final item in decoded) {
        if (item is Map<String, dynamic>) {
          accounts.add(Account.fromJson(item));
        }
      }

      accounts.sort(_compareAccounts);
      return accounts;
    } catch (e) {
      return [];
    }
  }

  Future<List<Account>> saveAccounts(List<Account> accounts) async {
    try {
      final file = await _getFile();
      final sorted = List<Account>.from(accounts)..sort(_compareAccounts);
      final jsonList = sorted.map((a) => a.toJson()).toList();
      await file.writeAsString(jsonEncode(jsonList));
      return sorted;
    } catch (e) {
      return accounts;
    }
  }

  Future<Account?> createAccount({
    required String label,
    required String accentColorHex,
    required int maxAccounts,
  }) async {
    final accounts = await loadAccounts();

    if (!canAddAccount(accounts, maxAccounts)) {
      return null;
    }

    final slot = getNextFreeSlot(accounts);
    if (slot == null) {
      return null;
    }

    final now = DateTime.now().millisecondsSinceEpoch;

    final account = Account(
      id: _uuid.v4(),
      label: label,
      accentColorHex: accentColorHex,
      processSlot: slot,
      createdAt: now,
      lastActiveAt: now,
      state: 'COLD',
      unreadCount: 0,
      sortOrder: accounts.length,
      snapshotPath: null,
      snapshotTimestamp: null,
      scrollPositionY: 0.0,
      hasNotification: false,
      totalInteractions: 0,
    );

    accounts.add(account);
    await saveAccounts(accounts);

    return account;
  }

  Future<Account?> updateAccount(Account updated) async {
    final accounts = await loadAccounts();

    final index = accounts.indexWhere((a) => a.id == updated.id);
    if (index == -1) {
      return null;
    }

    accounts[index] = updated;
    await saveAccounts(accounts);

    return updated;
  }

  Future<bool> deleteAccount(String id) async {
    final accounts = await loadAccounts();

    final initialLength = accounts.length;
    accounts.removeWhere((a) => a.id == id);

    if (accounts.length == initialLength) {
      return false;
    }

    for (var i = 0; i < accounts.length; i++) {
      if (accounts[i].sortOrder != i) {
        accounts[i] = accounts[i].copyWith(sortOrder: i);
      }
    }

    await saveAccounts(accounts);
    return true;
  }

  int? getNextFreeSlot(List<Account> accounts) {
    final usedSlots = accounts.map((a) => a.processSlot).toSet();

    for (var slot = _minSlot; slot <= _maxSlot; slot++) {
      if (!usedSlots.contains(slot)) {
        return slot;
      }
    }

    return null;
  }

  bool canAddAccount(List<Account> accounts, int maxAccounts) {
    if (accounts.length >= maxAccounts) {
      return false;
    }

    final slot = getNextFreeSlot(accounts);
    return slot != null;
  }

  int _compareAccounts(Account a, Account b) {
    final sortCompare = a.sortOrder.compareTo(b.sortOrder);
    if (sortCompare != 0) {
      return sortCompare;
    }
    return a.createdAt.compareTo(b.createdAt);
  }
}
