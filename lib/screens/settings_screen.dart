import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wault/models/account.dart';
import 'package:wault/services/account_service.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/utils/constants.dart';

class SettingsScreen extends StatefulWidget {
  final VoidCallback? onBack;

  const SettingsScreen({super.key, this.onBack});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  static const String _keyBlurEffects = 'pref_blur_effects';
  static const String _keyGlowAnimations = 'pref_glow_animations';

  final AccountService _accountService = AccountService();
  List<Account> _accounts = [];
  bool _blurEffects = true;
  bool _glowAnimations = true;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    final prefs = await SharedPreferences.getInstance();
    final accounts = await _accountService.loadAccounts();
    if (!mounted) return;
    setState(() {
      _blurEffects = prefs.getBool(_keyBlurEffects) ?? true;
      _glowAnimations = prefs.getBool(_keyGlowAnimations) ?? true;
      _accounts = accounts;
      _loading = false;
    });
  }

  Future<void> _setBlurEffects(bool value) async {
    setState(() {
      _blurEffects = value;
    });
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyBlurEffects, value);
  }

  Future<void> _setGlowAnimations(bool value) async {
    setState(() {
      _glowAnimations = value;
    });
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyGlowAnimations, value);
  }

  Future<void> _reloadAccounts() async {
    final accounts = await _accountService.loadAccounts();
    if (!mounted) return;
    setState(() {
      _accounts = accounts;
    });
  }

  void _showRenameDialog(Account account) {
    final controller = TextEditingController(text: account.label);

    showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16.0),
          ),
          title: Text(
            'Rename Account',
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 18.0,
              fontWeight: FontWeight.w600,
            ),
          ),
          content: TextField(
            controller: controller,
            autofocus: true,
            style: TextStyle(color: WaultColors.textPrimary, fontSize: 16.0),
            decoration: const InputDecoration(hintText: 'Account name'),
            textCapitalization: TextCapitalization.words,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(
                'Cancel',
                style: TextStyle(color: WaultColors.textSecondary),
              ),
            ),
            TextButton(
              onPressed: () {
                final trimmed = controller.text.trim();
                if (trimmed.isEmpty) return;
                Navigator.of(dialogContext).pop();
                _renameAccount(account, trimmed);
              },
              child: Text('Save', style: TextStyle(color: WaultColors.primary)),
            ),
          ],
        );
      },
    ).then((_) => controller.dispose());
  }

  Future<void> _renameAccount(Account account, String newName) async {
    final updated = account.copyWith(label: newName);
    await _accountService.updateAccount(updated);
    await _reloadAccounts();
  }

  void _showDeleteConfirmation(Account account) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          backgroundColor: WaultColors.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16.0),
          ),
          title: Text(
            'Delete Account',
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 18.0,
              fontWeight: FontWeight.w600,
            ),
          ),
          content: Text(
            'Are you sure you want to delete "${account.label}"? This cannot be undone.',
            style: TextStyle(color: WaultColors.textSecondary, fontSize: 15.0),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(
                'Cancel',
                style: TextStyle(color: WaultColors.textSecondary),
              ),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                _deleteAccount(account.id);
              },
              child: Text('Delete', style: TextStyle(color: WaultColors.error)),
            ),
          ],
        );
      },
    );
  }

  Future<void> _deleteAccount(String id) async {
    await _accountService.deleteAccount(id);
    await _reloadAccounts();
  }

  Color _parseHexColor(String hex) {
    try {
      final cleaned = hex.replaceFirst('#', '');
      if (cleaned.length == 6) {
        return Color(int.parse('FF$cleaned', radix: 16));
      }
      if (cleaned.length == 8) {
        return Color(int.parse(cleaned, radix: 16));
      }
    } catch (_) {}
    return WaultColors.primary;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: WaultColors.background,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: _loading
                  ? const SizedBox.shrink()
                  : ListView(
                      padding: const EdgeInsets.only(bottom: 40.0),
                      children: [
                        _buildSectionHeader('Appearance'),
                        _buildToggleTile(
                          icon: Icons.blur_on_rounded,
                          label: 'Blur Effects',
                          subtitle: 'Glass blur on cards and surfaces',
                          value: _blurEffects,
                          onChanged: _setBlurEffects,
                        ),
                        _buildToggleTile(
                          icon: Icons.auto_awesome_outlined,
                          label: 'Glow Animations',
                          subtitle: 'Accent glow and ring effects',
                          value: _glowAnimations,
                          onChanged: _setGlowAnimations,
                        ),
                        const SizedBox(height: 8.0),
                        _buildSectionHeader(
                          'Accounts (${_accounts.length}/${WaultConstants.defaultMaxAccounts})',
                        ),
                        if (_accounts.isEmpty)
                          _buildEmptyAccountsHint()
                        else
                          ..._accounts.map(_buildAccountTile),
                        const SizedBox(height: 8.0),
                        _buildSectionHeader('About'),
                        _buildAboutSection(),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 8.0),
      child: Row(
        children: [
          IconButton(
            onPressed: widget.onBack ?? () => Navigator.of(context).pop(),
            icon: Icon(
              Icons.arrow_back_rounded,
              color: WaultColors.textPrimary,
              size: 24.0,
            ),
          ),
          const SizedBox(width: 8.0),
          Text(
            'Settings',
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 22.0,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(
        left: 20.0,
        right: 20.0,
        top: 16.0,
        bottom: 8.0,
      ),
      child: Text(
        title,
        style: TextStyle(
          color: WaultColors.textTertiary,
          fontSize: 13.0,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.5,
        ),
      ),
    );
  }

  Widget _buildToggleTile({
    required IconData icon,
    required String label,
    required String subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 2.0),
      child: Container(
        decoration: BoxDecoration(
          color: WaultColors.surface,
          borderRadius: BorderRadius.circular(14.0),
          border: Border.all(color: WaultColors.glassBorder, width: 0.5),
        ),
        child: SwitchListTile(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16.0,
            vertical: 4.0,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14.0),
          ),
          secondary: Icon(icon, color: WaultColors.textSecondary, size: 22.0),
          title: Text(
            label,
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 15.0,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(
            subtitle,
            style: TextStyle(color: WaultColors.textTertiary, fontSize: 12.0),
          ),
          value: value,
          onChanged: (v) => onChanged(v),
          activeColor: WaultColors.primary,
          inactiveThumbColor: WaultColors.textTertiary,
          inactiveTrackColor: WaultColors.surfaceElevated,
        ),
      ),
    );
  }

  Widget _buildEmptyAccountsHint() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 12.0),
      child: Text(
        'No accounts created yet.',
        style: TextStyle(color: WaultColors.textTertiary, fontSize: 14.0),
      ),
    );
  }

  Widget _buildAccountTile(Account account) {
    final accentColor = _parseHexColor(account.accentColorHex);
    final initial = account.label.isNotEmpty
        ? account.label[0].toUpperCase()
        : '?';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 2.0),
      child: Container(
        decoration: BoxDecoration(
          color: WaultColors.surface,
          borderRadius: BorderRadius.circular(14.0),
          border: Border.all(color: WaultColors.glassBorder, width: 0.5),
        ),
        child: ListTile(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16.0,
            vertical: 4.0,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14.0),
          ),
          leading: CircleAvatar(
            radius: 18.0,
            backgroundColor: accentColor.withOpacity(0.15),
            child: Text(
              initial,
              style: TextStyle(
                color: accentColor,
                fontSize: 16.0,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          title: Text(
            account.label,
            style: TextStyle(
              color: WaultColors.textPrimary,
              fontSize: 15.0,
              fontWeight: FontWeight.w500,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          subtitle: Text(
            'Slot ${account.processSlot}',
            style: TextStyle(color: WaultColors.textTertiary, fontSize: 12.0),
          ),
          trailing: PopupMenuButton<String>(
            icon: Icon(
              Icons.more_vert_rounded,
              color: WaultColors.textSecondary,
              size: 20.0,
            ),
            color: WaultColors.surfaceElevated,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12.0),
            ),
            onSelected: (value) {
              if (value == 'rename') {
                _showRenameDialog(account);
              } else if (value == 'delete') {
                _showDeleteConfirmation(account);
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'rename',
                child: Text(
                  'Rename',
                  style: TextStyle(color: WaultColors.textPrimary),
                ),
              ),
              PopupMenuItem(
                value: 'delete',
                child: Text(
                  'Delete',
                  style: TextStyle(color: WaultColors.error),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAboutSection() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20.0),
        decoration: BoxDecoration(
          color: WaultColors.surface,
          borderRadius: BorderRadius.circular(14.0),
          border: Border.all(color: WaultColors.glassBorder, width: 0.5),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              WaultConstants.appName,
              style: TextStyle(
                color: WaultColors.textPrimary,
                fontSize: 18.0,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 4.0),
            Text(
              WaultConstants.appSubtitle,
              style: TextStyle(
                color: WaultColors.textSecondary,
                fontSize: 14.0,
              ),
            ),
            const SizedBox(height: 12.0),
            Divider(color: WaultColors.divider, height: 1.0),
            const SizedBox(height: 12.0),
            _buildInfoRow('Package', WaultConstants.packageName),
            const SizedBox(height: 6.0),
            _buildInfoRow('Version', '1.0.0'),
            const SizedBox(height: 6.0),
            _buildInfoRow(
              'Accounts',
              '${_accounts.length}/${WaultConstants.defaultMaxAccounts}',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      children: [
        Text(
          '$label: ',
          style: TextStyle(color: WaultColors.textTertiary, fontSize: 13.0),
        ),
        Text(
          value,
          style: TextStyle(color: WaultColors.textSecondary, fontSize: 13.0),
        ),
      ],
    );
  }
}
