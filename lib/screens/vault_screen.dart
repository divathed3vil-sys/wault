// File: lib/screens/vault_screen.dart
import 'dart:async';

import 'package:flutter/material.dart';

import '../models/account.dart';
import '../screens/settings_screen.dart';
import '../services/account_service.dart';
import '../services/engine_service.dart';
import '../theme/wault_colors.dart';
import '../utils/constants.dart';
import '../widgets/account_card.dart';
import '../widgets/account_options_sheet.dart';
import '../widgets/add_account_sheet.dart';
import '../widgets/empty_vault.dart';
import '../widgets/shield_logo.dart';
import '../widgets/wault_fab.dart';

class VaultScreen extends StatefulWidget {
  const VaultScreen({super.key});

  @override
  State<VaultScreen> createState() => _VaultScreenState();
}

class _VaultScreenState extends State<VaultScreen> with WidgetsBindingObserver {
  final AccountService _accountService = AccountService();
  final EngineService _engineService = EngineService.instance;

  List<Account> _accounts = <Account>[];
  bool _loading = true;
  String _selectedColorHex = WaultAccentHex.palette[0];
  StreamSubscription<Map<String, dynamic>>? _eventSubscription;
  int _maxAccounts = WaultConstants.defaultMaxAccounts;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _eventSubscription = _engineService.sessionEvents.listen(
      _handleSessionEvent,
    );
    _loadAccounts();
    _loadDeviceInfo();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _eventSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadAccounts();
    }
  }

  Future<void> _loadDeviceInfo() async {
    try {
      final Map<String, dynamic> info = await _engineService.getDeviceInfo();
      final dynamic maxAccounts = info['maxAccounts'];

      if (maxAccounts is int && mounted) {
        setState(() {
          _maxAccounts = maxAccounts;
        });
      }
    } catch (_) {}
  }

  Future<void> _handleSessionEvent(Map<String, dynamic> event) async {
    final String type = (event['type'] ?? '').toString();
    final String accountId = (event['accountId'] ?? '').toString();

    if (accountId.isEmpty) {
      return;
    }

    final int index = _accounts.indexWhere((Account a) => a.id == accountId);
    if (index == -1) {
      return;
    }

    Account updated = _accounts[index];

    if (type == 'unreadCount') {
      final dynamic countValue = event['count'];
      final int count =
          countValue is int
              ? countValue
              : int.tryParse(countValue?.toString() ?? '') ?? 0;

      updated = updated.copyWith(unreadCount: count);
    } else if (type == 'qrVisible') {
      updated = updated.copyWith(state: 'COLD');
    } else if (type == 'loggedIn') {
      updated = updated.copyWith(state: 'ACTIVE');
    } else if (type == 'sessionCrashed') {
      updated = updated.copyWith(state: 'ERROR');

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Session interrupted'),
            backgroundColor: WaultColors.error,
            duration: Duration(seconds: 2),
          ),
        );
      }
    } else if (type == 'sessionStateChanged') {
      final String newState = (event['state'] ?? '').toString();
      if (newState.isNotEmpty) {
        updated = updated.copyWith(state: newState);
      }
    } else if (type == 'sessionError') {
      updated = updated.copyWith(state: 'ERROR');

      if (mounted) {
        final String message = (event['message'] ?? 'Session error').toString();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(message),
            backgroundColor: WaultColors.error,
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } else {
      return;
    }

    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _loadAccounts() async {
    final List<Account> accounts = await _accountService.loadAccounts();
    if (!mounted) return;

    setState(() {
      _accounts = accounts;
      _loading = false;
    });
  }

  Future<void> _openSession(Account account) async {
    final Account updated = account.copyWith(
      lastActiveAt: DateTime.now().millisecondsSinceEpoch,
      state: 'ACTIVE',
    );

    await _accountService.updateAccount(updated);

    try {
      await _engineService.openSession(updated);
    } catch (_) {
      final Account errored = updated.copyWith(state: 'ERROR');
      await _accountService.updateAccount(errored);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Failed to open session. Please try again.'),
            duration: Duration(seconds: 2),
            backgroundColor: WaultColors.error,
          ),
        );
      }
    }

    await _loadAccounts();
  }

  Future<void> _addAccountFromSheet(String name) async {
    final Account? created = await _accountService.createAccount(
      label: name,
      accentColorHex: _selectedColorHex,
      maxAccounts: _maxAccounts,
    );

    if (!mounted) return;
    Navigator.of(context).pop();

    if (created != null) {
      await _loadAccounts();
      await _openSession(created);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Maximum of $_maxAccounts accounts reached'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  void _showAddAccountSheet() {
    if (_accounts.length >= _maxAccounts) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Maximum of $_maxAccounts accounts reached'),
          duration: const Duration(seconds: 2),
        ),
      );
      return;
    }

    _selectedColorHex = WaultAccentHex.palette[0];

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext sheetContext) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setSheetState) {
            return AddAccountSheet(
              accentPalette: WaultAccentHex.palette,
              initialSelectedColorHex: _selectedColorHex,
              onColorSelected: (String hex) {
                setSheetState(() {
                  _selectedColorHex = hex;
                });
              },
              onCreate: _addAccountFromSheet,
            );
          },
        );
      },
    );
  }

  Future<void> _renameAccount(Account account) async {
    final TextEditingController controller = TextEditingController(
      text: account.label,
    );

    final String? newLabel = await showDialog<String>(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          title: const Text(
            'Rename Account',
            style: TextStyle(color: WaultColors.textPrimary),
          ),
          content: TextField(
            controller: controller,
            autofocus: true,
            style: const TextStyle(color: WaultColors.textPrimary),
            decoration: const InputDecoration(hintText: 'Account name'),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed:
                  () => Navigator.of(dialogContext).pop(controller.text.trim()),
              child: const Text('Save'),
            ),
          ],
        );
      },
    );

    controller.dispose();

    if (newLabel == null || newLabel.isEmpty || newLabel == account.label) {
      return;
    }

    final Account updated = account.copyWith(label: newLabel);
    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _changeColor(Account account) async {
    final String? picked = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: WaultColors.surface,
      builder: (BuildContext sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children:
                  WaultAccentHex.palette.map((String hex) {
                    final Color color = Color(
                      int.parse('0xFF${hex.replaceFirst('#', '')}'),
                    );
                    final bool isSelected = account.accentColorHex == hex;

                    return GestureDetector(
                      onTap: () => Navigator.of(sheetContext).pop(hex),
                      child: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: color,
                          shape: BoxShape.circle,
                          border:
                              isSelected
                                  ? Border.all(color: Colors.white, width: 3)
                                  : null,
                        ),
                      ),
                    );
                  }).toList(),
            ),
          ),
        );
      },
    );

    if (picked == null || picked == account.accentColorHex) {
      return;
    }

    final Account updated = account.copyWith(accentColorHex: picked);
    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _deleteAccount(Account account) async {
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          title: const Text(
            'Delete Account',
            style: TextStyle(color: WaultColors.textPrimary),
          ),
          content: Text(
            'This will remove "${account.label}" from your vault and close its current session. '
            'Slot ${account.processSlot} will become available for reuse.',
            style: const TextStyle(color: WaultColors.textSecondary),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text(
                'Delete',
                style: TextStyle(color: WaultColors.error),
              ),
            ),
          ],
        );
      },
    );

    if (confirmed != true) return;

    await _engineService.closeSession(account.id);
    await _accountService.deleteAccount(account.id);
    await _loadAccounts();

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '"${account.label}" deleted. Slot ${account.processSlot} is now free.',
        ),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _showAccountOptions(Account account) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (BuildContext sheetContext) {
        return AccountOptionsSheet(
          accountLabel: account.label,
          onRename: () async {
            Navigator.of(sheetContext).pop();
            await _renameAccount(account);
          },
          onChangeColor: () async {
            Navigator.of(sheetContext).pop();
            await _changeColor(account);
          },
          onDelete: () async {
            Navigator.of(sheetContext).pop();
            await _deleteAccount(account);
          },
        );
      },
    );
  }

  void _openSettings() {
    Navigator.push(
      context,
      MaterialPageRoute<void>(builder: (_) => const SettingsScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bool canAdd = _accounts.length < _maxAccounts;

    return Scaffold(
      backgroundColor: WaultColors.background,
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: const <Widget>[
            ShieldLogo(size: 28),
            SizedBox(width: 10),
            Text('WAult'),
          ],
        ),
        centerTitle: true,
        backgroundColor: WaultColors.background,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: <Widget>[
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: _openSettings,
          ),
        ],
      ),
      body:
          _loading
              ? const Center(
                child: CircularProgressIndicator(color: WaultColors.primary),
              )
              : _accounts.isEmpty
              ? EmptyVault(onAddAccount: _showAddAccountSheet)
              : _buildAccountList(),
      floatingActionButton:
          canAdd ? WaultFab(onPressed: _showAddAccountSheet) : null,
    );
  }

  Widget _buildAccountList() {
    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: _accounts.length,
      itemBuilder: (BuildContext context, int index) {
        final Account account = _accounts[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: AccountCard(
            account: account,
            onTap: () => _openSession(account),
            onLongPress: () => _showAccountOptions(account),
          ),
        );
      },
    );
  }
}
