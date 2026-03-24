import 'package:flutter/material.dart';
import 'package:wault/theme/wault_colors.dart';

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
          _buildDragHandle(),
          const SizedBox(height: 16.0),
          _buildHeader(),
          const SizedBox(height: 12.0),
          Divider(color: WaultColors.divider, height: 1.0),
          const SizedBox(height: 4.0),
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
          Divider(color: WaultColors.divider, height: 1.0),
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
        width: 40.0,
        height: 4.0,
        decoration: BoxDecoration(
          color: WaultColors.glassBorder,
          borderRadius: BorderRadius.circular(2.0),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Text(
      accountLabel,
      style: TextStyle(
        color: WaultColors.textPrimary,
        fontSize: 18.0,
        fontWeight: FontWeight.w600,
      ),
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
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
        borderRadius: BorderRadius.circular(12.0),
        splashColor: WaultColors.glassHighlight,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4.0, vertical: 14.0),
          child: Row(
            children: [
              Icon(icon, color: color, size: 22.0),
              const SizedBox(width: 14.0),
              Expanded(
                child: Text(
                  label,
                  style: TextStyle(
                    color: color,
                    fontSize: 16.0,
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
