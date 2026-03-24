// lib/screens/settings_screen.dart

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wault/models/account.dart';
import 'package:wault/services/account_service.dart';
import 'package:wault/services/engine_service.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/widgets/shield_logo.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final AccountService _accountService = AccountService();
  final EngineService _engineService = EngineService.instance;

  List<Account> _accounts = [];
  bool _isLoading = true;
  bool _darkModeEnabled = true;
  Map<String, dynamic> _deviceInfo = {};

  @override
  void initState() {
    super.initState();
    _loadSettings();
    _loadAccounts();
    _loadDeviceInfo();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      _darkModeEnabled = prefs.getBool('darkModeEnabled') ?? true;
    });
  }

  Future<void> _loadAccounts() async {
    final accounts = await _accountService.loadAccounts();
    if (!mounted) return;
    setState(() {
      _accounts = accounts;
      _isLoading = false;
    });
  }

  Future<void> _loadDeviceInfo() async {
    final info = await _engineService.getDeviceInfo();
    if (!mounted) return;
    setState(() {
      _deviceInfo = info;
    });
  }

  Future<void> _toggleDarkMode(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('darkModeEnabled', value);
    if (!mounted) return;
    setState(() {
      _darkModeEnabled = value;
    });
  }

  Future<void> _deleteAllAccounts() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: WaultColors.surface,
        title: const Text(
          'Delete All Accounts',
          style: TextStyle(color: WaultColors.textPrimary),
        ),
        content: const Text(
          'This will permanently delete all accounts and close all sessions. This action cannot be undone.',
          style: TextStyle(color: WaultColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text(
              'Cancel',
              style: TextStyle(color: WaultColors.textSecondary),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text(
              'Delete All',
              style: TextStyle(color: WaultColors.error),
            ),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    await _engineService.closeAllSessions();
    for (final account in _accounts) {
      await _accountService.deleteAccount(account.id);
    }
    await _loadAccounts();

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('All accounts deleted'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final tier = _deviceInfo['tier']?.toString() ?? 'UNKNOWN';
    final maxAccounts = _deviceInfo['maxAccounts']?.toString() ?? '-';
    final totalRam = _deviceInfo['totalRamMB']?.toString() ?? '-';

    return Scaffold(
      backgroundColor: WaultColors.background,
      appBar: AppBar(
        backgroundColor: WaultColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: WaultColors.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Settings',
          style: TextStyle(
            color: WaultColors.textPrimary,
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: WaultColors.primary),
            )
          : ListView(
              padding: const EdgeInsets.symmetric(vertical: 16),
              children: [
                _buildSection(
                  title: 'Appearance',
                  children: [
                    _buildSwitchTile(
                      icon: Icons.dark_mode,
                      title: 'Dark Mode',
                      subtitle: 'Use dark theme',
                      value: _darkModeEnabled,
                      onChanged: _toggleDarkMode,
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                _buildSection(
                  title: 'Account Management',
                  children: [
                    _buildInfoTile(
                      icon: Icons.account_circle,
                      title: 'Saved Accounts',
                      subtitle: '${_accounts.length} account(s)',
                    ),
                    _buildActionTile(
                      icon: Icons.delete_sweep,
                      title: 'Delete All Accounts',
                      subtitle: 'Remove all accounts and close all sessions',
                      onTap: _deleteAllAccounts,
                      isDestructive: true,
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                _buildSection(
                  title: 'Device Info',
                  children: [
                    _buildInfoTile(
                      icon: Icons.memory,
                      title: 'Tier',
                      subtitle: tier,
                    ),
                    _buildInfoTile(
                      icon: Icons.storage,
                      title: 'Total RAM',
                      subtitle: '$totalRam MB',
                    ),
                    _buildInfoTile(
                      icon: Icons.layers,
                      title: 'Max Accounts',
                      subtitle: maxAccounts,
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                _buildSection(title: 'About', children: [_buildAboutTile()]),
              ],
            ),
    );
  }

  Widget _buildSection({
    required String title,
    required List<Widget> children,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: WaultColors.textSecondary,
              letterSpacing: 0.5,
            ),
          ),
        ),
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 16),
          decoration: BoxDecoration(
            color: WaultColors.surface,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(children: children),
        ),
      ],
    );
  }

  Widget _buildSwitchTile({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return ListTile(
      leading: Icon(icon, color: WaultColors.primary),
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
          color: WaultColors.textPrimary,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: const TextStyle(fontSize: 14, color: WaultColors.textSecondary),
      ),
      trailing: Switch(
        value: value,
        onChanged: onChanged,
        activeColor: WaultColors.primary,
      ),
    );
  }

  Widget _buildInfoTile({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return ListTile(
      leading: Icon(icon, color: WaultColors.primary),
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
          color: WaultColors.textPrimary,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: const TextStyle(fontSize: 14, color: WaultColors.textSecondary),
      ),
    );
  }

  Widget _buildActionTile({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
    bool isDestructive = false,
  }) {
    final color = isDestructive ? WaultColors.error : WaultColors.primary;

    return ListTile(
      leading: Icon(icon, color: color),
      title: Text(
        title,
        style: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
          color: color,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: const TextStyle(fontSize: 14, color: WaultColors.textSecondary),
      ),
      onTap: onTap,
    );
  }

  Widget _buildAboutTile() {
    return const ListTile(
      leading: ShieldLogo(size: 32),
      title: Text(
        'WAult',
        style: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: WaultColors.textPrimary,
        ),
      ),
      subtitle: Text(
        'a project by DIVA\nVersion 1.0.0',
        style: TextStyle(fontSize: 14, color: WaultColors.textSecondary),
      ),
      isThreeLine: true,
    );
  }
}
