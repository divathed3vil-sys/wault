// File: lib/widgets/account_card.dart
import 'package:flutter/material.dart';

import '../models/account.dart';
import '../theme/wault_colors.dart';
import '../utils/time_utils.dart';
import 'liquid_glass_card.dart';

class AccountCard extends StatelessWidget {
  final Account account;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const AccountCard({
    super.key,
    required this.account,
    required this.onTap,
    this.onLongPress,
  });

  Color _parseHexColor(String hex) {
    try {
      final String cleaned = hex.replaceFirst('#', '');
      if (cleaned.length == 6) {
        return Color(int.parse('FF$cleaned', radix: 16));
      }
      if (cleaned.length == 8) {
        return Color(int.parse(cleaned, radix: 16));
      }
    } catch (_) {}
    return WaultColors.primary;
  }

  Color _stateColor() {
    switch (account.state) {
      case 'ACTIVE':
        return WaultColors.activeBlue;
      case 'ERROR':
        return WaultColors.error;
      case 'COLD':
      default:
        return WaultColors.textTertiary;
    }
  }

  Color _stateTextColor() {
    switch (account.state) {
      case 'ACTIVE':
        return WaultColors.activeBlue;
      case 'ERROR':
        return WaultColors.error;
      case 'COLD':
      default:
        return WaultColors.textSecondary;
    }
  }

  String _stateLabel() {
    switch (account.state) {
      case 'ACTIVE':
        return 'Active';
      case 'ERROR':
        return 'Needs attention';
      case 'COLD':
      default:
        return 'Tap to open';
    }
  }

  String _unreadText(int count) {
    if (count > 99) {
      return '99+';
    }
    return '$count';
  }

  @override
  Widget build(BuildContext context) {
    final Color accent = _parseHexColor(account.accentColorHex);
    final String initial =
        account.label.isNotEmpty ? account.label[0].toUpperCase() : '?';
    final bool hasUnread = account.unreadCount > 0;
    final bool hasLastActive = account.lastActiveAt > 0;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: LiquidGlassCard(
        onTap: onTap,
        onLongPress: onLongPress,
        accentColor: accent,
        showGlow: hasUnread || account.state == 'ACTIVE',
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: <Widget>[
              Row(
                children: <Widget>[
                  Stack(
                    clipBehavior: Clip.none,
                    children: <Widget>[
                      Container(
                        width: 52,
                        height: 52,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: accent.withOpacity(0.15),
                          border: Border.all(
                            color: accent.withOpacity(0.30),
                            width: 1.5,
                          ),
                        ),
                        child: Text(
                          initial,
                          style: TextStyle(
                            color: accent,
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Positioned(
                        right: -2,
                        bottom: -2,
                        child: Container(
                          width: 14,
                          height: 14,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: _stateColor(),
                            border: Border.all(
                              color: WaultColors.background,
                              width: 2,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          account.label,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: WaultColors.textPrimary,
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          _stateLabel(),
                          style: TextStyle(
                            color: _stateTextColor(),
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  if (hasUnread)
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 3,
                      ),
                      decoration: BoxDecoration(
                        color: WaultColors.primary,
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        _unreadText(account.unreadCount),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  else
                    const Icon(
                      Icons.chevron_right_rounded,
                      color: WaultColors.textTertiary,
                      size: 20,
                    ),
                ],
              ),
              if (hasLastActive) ...<Widget>[
                const SizedBox(height: 12),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: accent.withOpacity(0.05),
                  ),
                  child: Text(
                    'Last active ${TimeUtils.formatRelative(account.lastActiveAt)}',
                    style: const TextStyle(
                      color: WaultColors.textTertiary,
                      fontSize: 11,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
