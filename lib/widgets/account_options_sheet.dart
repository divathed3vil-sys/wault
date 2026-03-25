// File: lib/widgets/account_options_sheet.dart
import 'package:flutter/material.dart';

import '../theme/wault_colors.dart';

class AccountOptionsSheet extends StatelessWidget {
  final String accountLabel;
  final VoidCallback onRename;
  final VoidCallback onChangeColor;
  final VoidCallback onDelete;

  const AccountOptionsSheet({
    super.key,
    required this.accountLabel,
    required this.onRename,
    required this.onChangeColor,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 20, right: 20, top: 8, bottom: 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          _buildDragHandle(),
          const SizedBox(height: 16),
          _buildHeader(),
          const SizedBox(height: 12),
          const Divider(color: WaultColors.glassBorder, height: 1),
          const SizedBox(height: 4),
          _buildAction(
            icon: Icons.edit_outlined,
            label: 'Rename',
            color: WaultColors.textPrimary,
            onTap: onRename,
          ),
          _buildAction(
            icon: Icons.palette_outlined,
            label: 'Change Color',
            color: WaultColors.textPrimary,
            onTap: onChangeColor,
          ),
          const Divider(color: WaultColors.glassBorder, height: 1),
          _buildAction(
            icon: Icons.delete_outline_rounded,
            label: 'Delete',
            color: WaultColors.error,
            onTap: onDelete,
          ),
        ],
      ),
    );
  }

  Widget _buildDragHandle() {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: WaultColors.glassBorder,
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Text(
      accountLabel,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: const TextStyle(
        color: WaultColors.textPrimary,
        fontSize: 18,
        fontWeight: FontWeight.w600,
      ),
    );
  }

  Widget _buildAction({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        splashColor: WaultColors.glassHighlight,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 14),
          child: Row(
            children: <Widget>[
              Icon(icon, color: color, size: 22),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  label,
                  style: TextStyle(
                    color: color,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
