// lib/screens/vault_screen.dart

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:wault/models/account.dart';
import 'package:wault/screens/settings_screen.dart';
import 'package:wault/services/account_service.dart';
import 'package:wault/services/engine_service.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/utils/constants.dart';
import 'package:wault/widgets/account_card.dart';
import 'package:wault/widgets/account_options_sheet.dart';
import 'package:wault/widgets/add_account_sheet.dart';
import 'package:wault/widgets/empty_vault.dart';
import 'package:wault/widgets/shield_logo.dart';
import 'package:wault/widgets/wault_fab.dart';

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
  StreamSubscription<SessionEvent>? _eventSubscription;
  int _maxAccounts = WaultConstants.defaultMaxAccounts;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _engineService.initialize();
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
    final info = await _engineService.getDeviceInfo();
    final maxAccounts = info['maxAccounts'];
    if (maxAccounts is int && mounted) {
      setState(() {
        _maxAccounts = maxAccounts;
      });
    }
  }

  Future<void> _handleSessionEvent(SessionEvent event) async {
    final index = _accounts.indexWhere((a) => a.id == event.accountId);
    if (index == -1) return;

    var updated = _accounts[index];

    if (event.isUnreadCount && event.count != null) {
      updated = updated.copyWith(unreadCount: event.count);
    } else if (event.isQrVisible) {
      updated = updated.copyWith(state: 'COLD');
    } else if (event.isLoggedIn) {
      updated = updated.copyWith(state: 'ACTIVE');
    } else if (event.isSessionCrashed || event.isSessionError) {
      updated = updated.copyWith(state: 'ERROR');
    } else if (event.isStateChanged &&
        event.state != null &&
        event.state!.isNotEmpty) {
      updated = updated.copyWith(state: event.state);
    }

    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _loadAccounts() async {
    final accounts = await _accountService.loadAccounts();
    if (!mounted) return;
    setState(() {
      _accounts = accounts;
      _loading = false;
    });
  }

  Future<void> _openSession(Account account) async {
    final updated = account.copyWith(
      lastActiveAt: DateTime.now().millisecondsSinceEpoch,
      state: 'ACTIVE',
    );
    await _accountService.updateAccount(updated);

    final success = await _engineService.openSession(updated);
    if (!success && mounted) {
      final errored = updated.copyWith(state: 'ERROR');
      await _accountService.updateAccount(errored);

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Failed to open session'),
          duration: Duration(seconds: 2),
        ),
      );
    }

    await _loadAccounts();
  }

  Future<void> _addAccountFromSheet(String name) async {
    final created = await _accountService.createAccount(
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
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            return AddAccountSheet(
              accentPalette: WaultAccentHex.palette,
              initialSelectedColorHex: _selectedColorHex,
              onColorSelected: (hex) {
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
    final controller = TextEditingController(text: account.label);

    final newLabel = await showDialog<String>(
      context: context,
      builder: (dialogContext) {
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
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () =>
                  Navigator.of(dialogContext).pop(controller.text.trim()),
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

    final updated = account.copyWith(label: newLabel);
    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _changeColor(Account account) async {
    final picked = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: WaultColors.surface,
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Wrap(
              spacing: 12,
              runSpacing: 12,
              children: WaultAccentHex.palette.map((hex) {
                final color = Color(
                  int.parse('0xFF${hex.replaceFirst('#', '')}'),
                );
                final isSelected = account.accentColorHex == hex;
                return GestureDetector(
                  onTap: () => Navigator.of(sheetContext).pop(hex),
                  child: Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                      border: isSelected
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

    if (picked == null || picked == account.accentColorHex) return;

    final updated = account.copyWith(accentColorHex: picked);
    await _accountService.updateAccount(updated);
    await _loadAccounts();
  }

  Future<void> _deleteAccount(Account account) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          title: const Text(
            'Delete Account',
            style: TextStyle(color: WaultColors.textPrimary),
          ),
          content: Text(
            'Remove "${account.label}"? This cannot be undone.',
            style: const TextStyle(color: WaultColors.textSecondary),
          ),
          actions: [
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
  }

  void _showAccountOptions(Account account) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
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
    final canAdd = _accounts.length < _maxAccounts;

    return Scaffold(
      backgroundColor: WaultColors.background,
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: const [
            ShieldLogo(size: 28),
            SizedBox(width: 10),
            Text('WAult'),
          ],
        ),
        centerTitle: true,
        backgroundColor: WaultColors.background,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: _openSettings,
          ),
        ],
      ),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(color: WaultColors.primary),
            )
          : _accounts.isEmpty
          ? EmptyVault(onAddAccount: _showAddAccountSheet)
          : _buildAccountList(),
      floatingActionButton: canAdd
          ? WaultFab(onPressed: _showAddAccountSheet)
          : null,
    );
  }

  Widget _buildAccountList() {
    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: _accounts.length,
      itemBuilder: (context, index) {
        final account = _accounts[index];
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
