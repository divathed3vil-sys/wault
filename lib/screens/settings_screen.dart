// File: lib/screens/settings_screen.dart
import 'package:flutter/material.dart';

import '../models/account.dart';
import '../services/account_service.dart';
import '../services/engine_service.dart';
import '../theme/wault_colors.dart';
import '../utils/time_utils.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final AccountService _accountService = AccountService();
  final EngineService _engineService = EngineService.instance;

  List<Account> _accounts = <Account>[];
  Map<String, dynamic>? _deviceInfo;
  bool _isLoading = true;
  bool _isBusy = false;

  bool _focusedSessionStyling = true;

  static const List<String> _accentPalette = <String>[
    '#25D366',
    '#53BDEB',
    '#FF6B9D',
    '#FFB340',
    '#A78BFA',
    '#34D399',
    '#F472B6',
    '#60A5FA',
  ];

  @override
  void initState() {
    super.initState();
    _loadScreenData();
  }

  Future<void> _loadScreenData() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final List<Account> accounts = await _accountService.loadAccounts();
      Map<String, dynamic>? deviceInfo;

      try {
        deviceInfo = await _engineService.getDeviceInfo();
      } catch (_) {
        deviceInfo = null;
      }

      if (!mounted) return;

      setState(() {
        _accounts = accounts;
        _deviceInfo = deviceInfo;
        _focusedSessionStyling = true;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _accounts = <Account>[];
        _deviceInfo = null;
        _focusedSessionStyling = true;
        _isLoading = false;
      });
    }
  }

  Future<void> _renameAccount(Account account) async {
    final TextEditingController controller = TextEditingController(
      text: account.label,
    );

    final String? newLabel = await showDialog<String>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: const Text('Rename account'),
          content: TextField(
            controller: controller,
            autofocus: true,
            style: const TextStyle(color: WaultColors.textPrimary),
            decoration: InputDecoration(
              hintText: 'Account name',
              hintStyle: const TextStyle(color: WaultColors.textTertiary),
              filled: true,
              fillColor: WaultColors.surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: BorderSide.none,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: WaultColors.glassBorder),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: WaultColors.primary),
              ),
            ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text(
                'Cancel',
                style: TextStyle(color: WaultColors.textSecondary),
              ),
            ),
            FilledButton(
              onPressed: () {
                final String value = controller.text.trim();
                if (value.isEmpty) return;
                Navigator.of(context).pop(value);
              },
              style: FilledButton.styleFrom(
                backgroundColor: WaultColors.primary,
                foregroundColor: Colors.white,
              ),
              child: const Text('Save'),
            ),
          ],
        );
      },
    );

    if (newLabel == null || newLabel.trim().isEmpty) {
      return;
    }

    final Account updated = account.copyWith(label: newLabel.trim());
    await _accountService.updateAccount(updated);
    await _loadScreenData();
  }

  Future<void> _changeAccountColor(Account account) async {
    String selectedColor = account.accentColorHex;

    final String? pickedColor = await showDialog<String>(
      context: context,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setModalState) {
            return AlertDialog(
              backgroundColor: WaultColors.surfaceElevated,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              title: const Text('Change accent color'),
              content: Wrap(
                spacing: 12,
                runSpacing: 12,
                children:
                    _accentPalette.map((String colorHex) {
                      final Color color = _colorFromHex(colorHex);
                      final bool isSelected = selectedColor == colorHex;

                      return GestureDetector(
                        onTap: () {
                          setModalState(() {
                            selectedColor = colorHex;
                          });
                        },
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: 42,
                          height: 42,
                          decoration: BoxDecoration(
                            color: color,
                            shape: BoxShape.circle,
                            border: Border.all(
                              color:
                                  isSelected
                                      ? Colors.white
                                      : Colors.white.withOpacity(0.20),
                              width: isSelected ? 3 : 1.2,
                            ),
                            boxShadow: <BoxShadow>[
                              if (isSelected)
                                BoxShadow(
                                  color: color.withOpacity(0.35),
                                  blurRadius: 14,
                                  spreadRadius: 1,
                                ),
                            ],
                          ),
                          child:
                              isSelected
                                  ? const Icon(
                                    Icons.check,
                                    color: Colors.white,
                                    size: 18,
                                  )
                                  : null,
                        ),
                      );
                    }).toList(),
              ),
              actions: <Widget>[
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text(
                    'Cancel',
                    style: TextStyle(color: WaultColors.textSecondary),
                  ),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(context).pop(selectedColor),
                  style: FilledButton.styleFrom(
                    backgroundColor: WaultColors.primary,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text('Apply'),
                ),
              ],
            );
          },
        );
      },
    );

    if (pickedColor == null || pickedColor == account.accentColorHex) {
      return;
    }

    final Account updated = account.copyWith(accentColorHex: pickedColor);
    await _accountService.updateAccount(updated);
    await _loadScreenData();
  }

  Future<void> _deleteAccount(Account account) async {
    final bool confirmed =
        await _showConfirmationDialog(
          title: 'Delete ${account.label}?',
          message:
              'This will remove the account from your vault. The slot will become available for reuse.',
          confirmLabel: 'Delete',
          isDestructive: true,
        ) ??
        false;

    if (!confirmed) return;

    await _engineService.closeSession(account.id);
    await _accountService.deleteAccount(account.id);
    await _loadScreenData();

    if (!mounted) return;
    _showSnackBar('${account.label} deleted');
  }

  Future<void> _deleteAllAccounts() async {
    if (_accounts.isEmpty) {
      _showSnackBar('No accounts to delete');
      return;
    }

    final bool confirmed =
        await _showConfirmationDialog(
          title: 'Delete all accounts?',
          message:
              'This will remove all saved accounts from WAult and free all process slots.',
          confirmLabel: 'Delete all',
          isDestructive: true,
        ) ??
        false;

    if (!confirmed) return;

    setState(() {
      _isBusy = true;
    });

    try {
      await _engineService.closeAllSessions();

      final List<Account> accountsSnapshot = List<Account>.from(_accounts);
      for (final Account account in accountsSnapshot) {
        await _accountService.deleteAccount(account.id);
      }

      await _loadScreenData();

      if (!mounted) return;
      _showSnackBar('All accounts deleted');
    } finally {
      if (!mounted) return;
      setState(() {
        _isBusy = false;
      });
    }
  }

  Future<bool?> _showConfirmationDialog({
    required String title,
    required String message,
    required String confirmLabel,
    bool isDestructive = false,
  }) {
    return showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: Text(title),
          content: Text(
            message,
            style: const TextStyle(color: WaultColors.textSecondary),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text(
                'Cancel',
                style: TextStyle(color: WaultColors.textSecondary),
              ),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: FilledButton.styleFrom(
                backgroundColor:
                    isDestructive ? WaultColors.error : WaultColors.primary,
                foregroundColor: Colors.white,
              ),
              child: Text(confirmLabel),
            ),
          ],
        );
      },
    );
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        backgroundColor: WaultColors.surface,
        content: Text(
          message,
          style: const TextStyle(color: WaultColors.textPrimary),
        ),
      ),
    );
  }

  Color _colorFromHex(String hex) {
    final String cleaned = hex.replaceFirst('#', '');
    return Color(int.parse('FF$cleaned', radix: 16));
  }

  String _deviceInfoLine(String label, String value) {
    return '$label: $value';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: WaultColors.background,
      body: SafeArea(
        child:
            _isLoading
                ? const Center(
                  child: CircularProgressIndicator(color: WaultColors.primary),
                )
                : Column(
                  children: <Widget>[
                    _buildTopBar(),
                    Expanded(
                      child: RefreshIndicator(
                        color: WaultColors.primary,
                        backgroundColor: WaultColors.surface,
                        onRefresh: _loadScreenData,
                        child: ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                          children: <Widget>[
                            _buildAppearanceSection(),
                            const SizedBox(height: 16),
                            _buildAccountsSection(),
                            const SizedBox(height: 16),
                            _buildDeviceInfoSection(),
                            const SizedBox(height: 16),
                            _buildAboutSection(),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
      ),
    );
  }

  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
      child: Row(
        children: <Widget>[
          _TopIconButton(
            icon: Icons.arrow_back_ios_new_rounded,
            onTap: () => Navigator.of(context).pop(),
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Text(
              'Settings',
              style: TextStyle(
                color: WaultColors.textPrimary,
                fontSize: 24,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          _TopIconButton(icon: Icons.refresh_rounded, onTap: _loadScreenData),
        ],
      ),
    );
  }

  Widget _buildAppearanceSection() {
    return _SettingsSectionCard(
      title: 'Session experience',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _focusedSessionStyling,
            activeColor: WaultColors.primary,
            title: const Text(
              'Focused Session Styling',
              style: TextStyle(
                color: WaultColors.textPrimary,
                fontSize: 15,
                fontWeight: FontWeight.w600,
              ),
            ),
            subtitle: const Padding(
              padding: EdgeInsets.only(top: 4),
              child: Text(
                'Applies WAult styling to WhatsApp Web sessions only. This does not change the app theme.',
                style: TextStyle(
                  color: WaultColors.textSecondary,
                  fontSize: 12.5,
                  height: 1.35,
                ),
              ),
            ),
            onChanged: (bool value) {
              setState(() {
                _focusedSessionStyling = value;
              });
              _showSnackBar(
                value
                    ? 'Focused session styling enabled'
                    : 'Focused session styling disabled',
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildAccountsSection() {
    return _SettingsSectionCard(
      title: 'Accounts',
      trailing:
          _accounts.isEmpty
              ? null
              : TextButton(
                onPressed: _isBusy ? null : _deleteAllAccounts,
                child: const Text(
                  'Delete all',
                  style: TextStyle(
                    color: WaultColors.error,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
      child:
          _accounts.isEmpty
              ? const Text(
                'No accounts added yet.',
                style: TextStyle(
                  color: WaultColors.textSecondary,
                  fontSize: 14,
                ),
              )
              : Column(
                children:
                    _accounts.map((Account account) {
                      final Color accent = _colorFromHex(
                        account.accentColorHex,
                      );

                      return Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: WaultColors.surface.withOpacity(0.55),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.06),
                          ),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Container(
                              width: 42,
                              height: 42,
                              decoration: BoxDecoration(
                                color: accent.withOpacity(0.16),
                                shape: BoxShape.circle,
                                border: Border.all(
                                  color: accent.withOpacity(0.35),
                                ),
                              ),
                              alignment: Alignment.center,
                              child: Text(
                                account.label.isNotEmpty
                                    ? account.label[0].toUpperCase()
                                    : '?',
                                style: TextStyle(
                                  color: accent,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 17,
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: <Widget>[
                                  Text(
                                    account.label,
                                    style: const TextStyle(
                                      color: WaultColors.textPrimary,
                                      fontSize: 15,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    'Slot ${account.processSlot} • ${account.state}',
                                    style: const TextStyle(
                                      color: WaultColors.textSecondary,
                                      fontSize: 12.5,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    account.lastActiveAt > 0
                                        ? 'Last active ${TimeUtils.formatRelative(account.lastActiveAt)}'
                                        : 'Never opened yet',
                                    style: const TextStyle(
                                      color: WaultColors.textTertiary,
                                      fontSize: 11.5,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            PopupMenuButton<String>(
                              color: WaultColors.surfaceElevated,
                              icon: const Icon(
                                Icons.more_vert_rounded,
                                color: WaultColors.textSecondary,
                              ),
                              onSelected: (String value) {
                                switch (value) {
                                  case 'rename':
                                    _renameAccount(account);
                                    break;
                                  case 'color':
                                    _changeAccountColor(account);
                                    break;
                                  case 'delete':
                                    _deleteAccount(account);
                                    break;
                                }
                              },
                              itemBuilder:
                                  (BuildContext context) =>
                                      <PopupMenuEntry<String>>[
                                        const PopupMenuItem<String>(
                                          value: 'rename',
                                          child: Text('Rename'),
                                        ),
                                        const PopupMenuItem<String>(
                                          value: 'color',
                                          child: Text('Change color'),
                                        ),
                                        const PopupMenuItem<String>(
                                          value: 'delete',
                                          child: Text(
                                            'Delete',
                                            style: TextStyle(
                                              color: WaultColors.error,
                                            ),
                                          ),
                                        ),
                                      ],
                            ),
                          ],
                        ),
                      );
                    }).toList(),
              ),
    );
  }

  Widget _buildDeviceInfoSection() {
    final Map<String, dynamic>? info = _deviceInfo;

    return _SettingsSectionCard(
      title: 'Device info',
      child:
          info == null
              ? const Text(
                'Device details are not available right now.',
                style: TextStyle(
                  color: WaultColors.textSecondary,
                  fontSize: 14,
                ),
              )
              : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  _InfoLine(
                    _deviceInfoLine('Tier', '${info['tier'] ?? 'Unknown'}'),
                  ),
                  _InfoLine(
                    _deviceInfoLine(
                      'Total RAM',
                      '${info['totalRamMB'] ?? '—'} MB',
                    ),
                  ),
                  _InfoLine(
                    _deviceInfoLine(
                      'Available RAM',
                      '${info['availableRamMB'] ?? '—'} MB',
                    ),
                  ),
                  _InfoLine(
                    _deviceInfoLine('CPU cores', '${info['cpuCores'] ?? '—'}'),
                  ),
                  _InfoLine(
                    _deviceInfoLine(
                      'Max accounts',
                      '${info['maxAccounts'] ?? '—'}',
                    ),
                  ),
                  _InfoLine(
                    _deviceInfoLine(
                      'Max warm sessions',
                      '${info['maxWarm'] ?? '—'}',
                    ),
                    isLast: true,
                  ),
                ],
              ),
    );
  }

  Widget _buildAboutSection() {
    return _SettingsSectionCard(
      title: 'About',
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'WAult',
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
          SizedBox(height: 4),
          Text(
            'a project by DIVA',
            style: TextStyle(color: WaultColors.textSecondary, fontSize: 13),
          ),
          SizedBox(height: 12),
          Text(
            'SAFE MVP build focused on multi-account session isolation, native session launching, and a premium dark vault experience.',
            style: TextStyle(
              color: WaultColors.textSecondary,
              fontSize: 13,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

class _TopIconButton extends StatelessWidget {
  const _TopIconButton({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: WaultColors.surface,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: SizedBox(
          width: 42,
          height: 42,
          child: Icon(icon, color: WaultColors.textPrimary, size: 20),
        ),
      ),
    );
  }
}

class _SettingsSectionCard extends StatelessWidget {
  const _SettingsSectionCard({
    required this.title,
    required this.child,
    this.trailing,
  });

  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: WaultColors.surface,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: Colors.white.withOpacity(0.07)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    color: WaultColors.textPrimary,
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              if (trailing != null) trailing!,
            ],
          ),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}

class _InfoLine extends StatelessWidget {
  const _InfoLine(this.text, {this.isLast = false});

  final String text;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: isLast ? 0 : 8),
      child: Text(
        text,
        style: const TextStyle(
          color: WaultColors.textSecondary,
          fontSize: 13.5,
          height: 1.35,
        ),
      ),
    );
  }
}
