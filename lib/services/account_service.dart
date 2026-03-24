// lib/services/account_service.dart

import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';
import 'package:wault/models/account.dart';

class AccountService {
  static const Uuid _uuid = Uuid();
  static const String _fileName = 'accounts.json';

  Future<File> _getAccountsFile() async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/$_fileName');
  }

  Future<List<Account>> loadAccounts() async {
    try {
      final file = await _getAccountsFile();
      if (!await file.exists()) {
        return <Account>[];
      }

      final raw = await file.readAsString();
      if (raw.trim().isEmpty) {
        return <Account>[];
      }

      final decoded = json.decode(raw);
      if (decoded is! List) {
        return <Account>[];
      }

      return decoded
          .map((item) => Account.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    } catch (_) {
      return <Account>[];
    }
  }

  Future<void> _saveAccounts(List<Account> accounts) async {
    final file = await _getAccountsFile();
    final jsonList = accounts.map((account) => account.toJson()).toList();
    await file.writeAsString(json.encode(jsonList), flush: true);
  }

  bool canAddAccount(List<Account> accounts, int maxAccounts) {
    return accounts.length < maxAccounts;
  }

  int? getNextFreeSlot(List<Account> accounts, int maxAccounts) {
    final usedSlots = accounts.map((a) => a.processSlot).toSet();
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
    final accounts = await loadAccounts();

    if (!canAddAccount(accounts, maxAccounts)) {
      return null;
    }

    final nextSlot = getNextFreeSlot(accounts, maxAccounts);
    if (nextSlot == null) {
      return null;
    }

    final now = DateTime.now().millisecondsSinceEpoch;

    final account = Account(
      id: _uuid.v4(),
      label: label,
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
    final accounts = await loadAccounts();
    final index = accounts.indexWhere((a) => a.id == updatedAccount.id);
    if (index == -1) return;

    accounts[index] = updatedAccount;
    await _saveAccounts(accounts);
  }

  Future<void> deleteAccount(String accountId) async {
    final accounts = await loadAccounts();
    accounts.removeWhere((account) => account.id == accountId);
    await _saveAccounts(accounts);
  }
}
