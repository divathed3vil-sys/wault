// File: lib/services/account_service.dart
import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../models/account.dart';

class AccountService {
  static const Uuid _uuid = Uuid();
  static const String _fileName = 'accounts.json';

  Future<File> _getAccountsFile() async {
    final Directory directory = await getApplicationDocumentsDirectory();

    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }

    return File('${directory.path}/$_fileName');
  }

  Future<List<Account>> loadAccounts() async {
    try {
      final File file = await _getAccountsFile();

      if (!await file.exists()) {
        return <Account>[];
      }

      final String raw = await file.readAsString();

      if (raw.trim().isEmpty) {
        return <Account>[];
      }

      final dynamic decoded = json.decode(raw);

      if (decoded is! List) {
        return <Account>[];
      }

      final List<Account> accounts =
          decoded
              .whereType<Object>()
              .map(
                (Object item) =>
                    Account.fromJson(Map<String, dynamic>.from(item as Map)),
              )
              .toList();

      accounts.sort((Account a, Account b) {
        return a.createdAt.compareTo(b.createdAt);
      });

      return accounts;
    } catch (_) {
      return <Account>[];
    }
  }

  Future<void> _saveAccounts(List<Account> accounts) async {
    final File file = await _getAccountsFile();
    final List<Map<String, dynamic>> jsonList =
        accounts.map((Account account) => account.toJson()).toList();

    await file.writeAsString(json.encode(jsonList), flush: true);
  }

  bool canAddAccount(List<Account> accounts, int maxAccounts) {
    return accounts.length < maxAccounts;
  }

  int? getNextFreeSlot(List<Account> accounts, int maxAccounts) {
    final Set<int> usedSlots =
        accounts.map((Account account) => account.processSlot).toSet();

    for (int i = 0; i < maxAccounts; i++) {
      if (!usedSlots.contains(i)) {
        return i;
      }
    }

    return null;
  }

  Future<Account?> createAccount({
    required String label,
    required String accentColorHex,
    required int maxAccounts,
  }) async {
    final List<Account> accounts = await loadAccounts();

    if (!canAddAccount(accounts, maxAccounts)) {
      return null;
    }

    final int? nextSlot = getNextFreeSlot(accounts, maxAccounts);
    if (nextSlot == null) {
      return null;
    }

    final int now = DateTime.now().millisecondsSinceEpoch;

    final Account account = Account(
      id: _uuid.v4(),
      label: label.trim(),
      accentColorHex: accentColorHex,
      processSlot: nextSlot,
      createdAt: now,
      lastActiveAt: now,
      state: 'COLD',
      unreadCount: 0,
    );

    accounts.add(account);
    await _saveAccounts(accounts);

    return account;
  }

  Future<void> updateAccount(Account updatedAccount) async {
    final List<Account> accounts = await loadAccounts();
    final int index = accounts.indexWhere(
      (Account account) => account.id == updatedAccount.id,
    );

    if (index == -1) {
      return;
    }

    accounts[index] = updatedAccount;
    await _saveAccounts(accounts);
  }

  Future<void> deleteAccount(String accountId) async {
    final List<Account> accounts = await loadAccounts();
    accounts.removeWhere((Account account) => account.id == accountId);
    await _saveAccounts(accounts);
  }
}
