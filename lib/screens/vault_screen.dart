import 'package:flutter/material.dart';
import 'package:wault/models/account.dart';
import 'package:wault/services/account_service.dart';
import 'package:wault/theme/wault_colors.dart';
import 'package:wault/utils/constants.dart';
import 'package:wault/widgets/empty_vault.dart';
import 'package:wault/widgets/account_card.dart';
import 'package:wault/widgets/wault_fab.dart';
import 'package:wault/widgets/add_account_sheet.dart';
import 'package:wault/widgets/account_options_sheet.dart';

class VaultScreen extends StatefulWidget {
  final VoidCallback? onOpenSettings;
  final ValueChanged<Account>? onOpenAccount;

  const VaultScreen({super.key, this.onOpenSettings, this.onOpenAccount});

  @override
  State<VaultScreen> createState() => _VaultScreenState();
}

class _VaultScreenState extends State<VaultScreen> {
  final AccountService _accountService = AccountService();
  List<Account> _accounts = [];
  bool _loading = true;
  String _selectedColorHex = WaultAccentHex.palette.first;

  @override
  void initState() {
    super.initState();
    _loadAccounts();
  }

  Future<void> _loadAccounts() async {
    final accounts = await _accountService.loadAccounts();
    if (!mounted) return;
    setState(() {
      _accounts = accounts;
      _loading = false;
    });
  }

  void _showAddAccountSheet() {
    _selectedColorHex = WaultAccentHex.palette.first;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: WaultColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20.0)),
      ),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (sbContext, setSheetState) {
            return AddAccountSheet(
              accentPalette: WaultAccentHex.palette,
              initialSelectedColorHex: _selectedColorHex,
              onColorSelected: (hex) {
                setSheetState(() {
                  _selectedColorHex = hex;
                });
              },
              onCreate: (name) {
                Navigator.of(sbContext).pop();
                _createAccount(name);
              },
            );
          },
        );
      },
    );
  }

  Future<void> _createAccount(String name) async {
    final account = await _accountService.createAccount(
      label: name,
      accentColorHex: _selectedColorHex,
      maxAccounts: WaultConstants.defaultMaxAccounts,
    );

    if (account == null) return;

    await _loadAccounts();

    if (!mounted) return;
    widget.onOpenAccount?.call(account);
  }

  void _showAccountOptions(Account account) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: WaultColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20.0)),
      ),
      builder: (sheetContext) {
        return AccountOptionsSheet(
          accountLabel: account.label,
          onRename: () {
            Navigator.of(sheetContext).pop();
            _showRenameDialog(account);
          },
          onChangeColor: () {
            Navigator.of(sheetContext).pop();
            _showChangeColorSheet(account);
          },
          onDelete: () {
            Navigator.of(sheetContext).pop();
            _showDeleteConfirmation(account);
          },
        );
      },
    );
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
    await _loadAccounts();
  }

  void _showChangeColorSheet(Account account) {
    String selectedHex = account.accentColorHex;

    showModalBottomSheet<void>(
      context: context,
      backgroundColor: WaultColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20.0)),
      ),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (sbContext, setSheetState) {
            return Padding(
              padding: const EdgeInsets.only(
                left: 20.0,
                right: 20.0,
                top: 8.0,
                bottom: 24.0,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40.0,
                      height: 4.0,
                      decoration: BoxDecoration(
                        color: WaultColors.glassBorder,
                        borderRadius: BorderRadius.circular(2.0),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16.0),
                  Text(
                    'Change Color',
                    style: TextStyle(
                      color: WaultColors.textPrimary,
                      fontSize: 20.0,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 16.0),
                  Wrap(
                    spacing: 12.0,
                    runSpacing: 12.0,
                    children: WaultAccentHex.palette.map((hex) {
                      final color = _parseHexColor(hex);
                      final isSelected = hex == selectedHex;
                      return GestureDetector(
                        onTap: () {
                          setSheetState(() {
                            selectedHex = hex;
                          });
                        },
                        child: Container(
                          width: 36.0,
                          height: 36.0,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: color,
                            border: isSelected
                                ? Border.all(
                                    color: WaultColors.textPrimary,
                                    width: 2.5,
                                  )
                                : Border.all(
                                    color: color.withOpacity(0.4),
                                    width: 1.5,
                                  ),
                          ),
                          child: isSelected
                              ? Icon(
                                  Icons.check_rounded,
                                  color: WaultColors.background,
                                  size: 20.0,
                                )
                              : null,
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 20.0),
                  SizedBox(
                    width: double.infinity,
                    child: TextButton(
                      onPressed: () {
                        Navigator.of(sbContext).pop();
                        _changeAccountColor(account, selectedHex);
                      },
                      style: TextButton.styleFrom(
                        backgroundColor: WaultColors.primary.withOpacity(0.15),
                        foregroundColor: WaultColors.primary,
                        padding: const EdgeInsets.symmetric(vertical: 14.0),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14.0),
                        ),
                      ),
                      child: Text(
                        'Apply',
                        style: TextStyle(
                          fontSize: 16.0,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _changeAccountColor(Account account, String hex) async {
    final updated = account.copyWith(accentColorHex: hex);
    await _accountService.updateAccount(updated);
    await _loadAccounts();
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
    await _loadAccounts();
  }

  void _handleAccountTap(Account account) {
    widget.onOpenAccount?.call(account);
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
    final canAdd = _accountService.canAddAccount(
      _accounts,
      WaultConstants.defaultMaxAccounts,
    );

    return Scaffold(
      backgroundColor: WaultColors.background,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: _loading
                  ? const SizedBox.shrink()
                  : _accounts.isEmpty
                  ? EmptyVault(onAddAccount: _showAddAccountSheet)
                  : _buildAccountList(),
            ),
          ],
        ),
      ),
      floatingActionButton: canAdd && !_loading
          ? WaultFab(onPressed: _showAddAccountSheet, tooltip: 'Add account')
          : null,
    );
  }

  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 12.0),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  WaultConstants.appName,
                  style: TextStyle(
                    color: WaultColors.textPrimary,
                    fontSize: 28.0,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 2.0),
                Text(
                  '${_accounts.length} of ${WaultConstants.defaultMaxAccounts} accounts',
                  style: TextStyle(
                    color: WaultColors.textTertiary,
                    fontSize: 13.0,
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: widget.onOpenSettings,
            icon: Icon(
              Icons.settings_outlined,
              color: WaultColors.textSecondary,
              size: 24.0,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAccountList() {
    return ListView.builder(
      padding: const EdgeInsets.only(top: 8.0, bottom: 100.0),
      itemCount: _accounts.length,
      itemBuilder: (context, index) {
        final account = _accounts[index];
        return AccountCard(
          account: account,
          onTap: () => _handleAccountTap(account),
          onLongPress: () => _showAccountOptions(account),
        );
      },
    );
  }
}
